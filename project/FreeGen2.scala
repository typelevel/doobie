import sbt._, Keys._
import java.lang.reflect._
import scala.reflect.ClassTag
import Predef._

object FreeGen2 {

  lazy val freeGen2Classes = settingKey[List[Class[_]]]("classes for which free algebras should be generated")
  lazy val freeGen2Dir = settingKey[File]("directory where free algebras go")
  lazy val freeGen2 = taskKey[Seq[File]]("generate free algebras")

  lazy val freeGen2Settings = Seq(
    freeGen2Classes := Nil,
    freeGen2Dir := (sourceManaged in Compile).value,
    freeGen2 := new FreeGen2(freeGen2Classes.value, state.value.log).gen(freeGen2Dir.value)
  )

}

class FreeGen2(managed: List[Class[_]], log: Logger) {

  // These Java classes will have non-Java names in our generated code
  val ClassBoolean  = classOf[Boolean]
  val ClassByte     = classOf[Byte]
  val ClassShort    = classOf[Short]
  val ClassInt      = classOf[Int]
  val ClassLong     = classOf[Long]
  val ClassFloat    = classOf[Float]
  val ClassDouble   = classOf[Double]
  val ClassObject   = classOf[Object]
  val ClassVoid     = Void.TYPE

  val renames: Map[Class[_], String] =
    Map(classOf[java.sql.Array] -> "SqlArray")

  def tparams(t: Type): List[String] =
    t match {
      case t: GenericArrayType  => tparams(t.getGenericComponentType)
      case t: ParameterizedType => t.getActualTypeArguments.toList.flatMap(tparams)
      case t: TypeVariable[_]   => List(t.toString)
      case _                    => Nil
    }

  def toScalaType(t: Type): String =
    t match {
      case t: GenericArrayType  => s"Array[${toScalaType(t.getGenericComponentType)}]"
      case t: ParameterizedType => s"${toScalaType(t.getRawType)}${t.getActualTypeArguments.map(toScalaType).mkString("[", ", ", "]")}"
      case t: WildcardType      => "_" // not quite right but ok
      case t: TypeVariable[_]   => t.toString
      case ClassVoid            => "Unit"
      case ClassBoolean         => "Boolean"
      case ClassByte            => "Byte"
      case ClassShort           => "Short"
      case ClassInt             => "Int"
      case ClassLong            => "Long"
      case ClassFloat           => "Float"
      case ClassDouble          => "Double"
      case ClassObject          => "AnyRef"
      case x: Class[_] if x.isArray => s"Array[${toScalaType(x.getComponentType)}]"
      case x: Class[_]          => renames.getOrElse(x, x.getSimpleName)
    }


  // Each constructor for our algebra maps to an underlying method, and an index is provided to
  // disambiguate in cases of overloading.
  case class Ctor(method: Method, index: Int) {

    // The method name, unchanged
    def mname: String =
      method.getName

    // The case class constructor name, capitalized and with an index when needed
    def cname: String = {
      val s = mname(0).toUpper +: mname.drop(1)
      (if (index == 0) s else s"$s$index")
    }

    // Constructor parameter type names
    def cparams: List[String] =
      method.getGenericParameterTypes.toList.map(toScalaType)

    def ctparams: String = {
      val ss = (method.getGenericParameterTypes.toList.flatMap(tparams) ++ tparams(method.getGenericReturnType)).toSet
      if (ss.isEmpty) "" else ss.mkString("[", ", ", "]")
    }

    // Constructor arguments, a .. z zipped with the right type
    def cargs: List[String] =
      "abcdefghijklmnopqrstuvwxyz".toList.zip(cparams).map {
        case (n, t) => s"$n: $t"
      }

    // Return type name
    def ret: String =
      toScalaType(method.getGenericReturnType)


    // Case class/object declaration
    def ctor(sname:String): String =
      ("|case " + (cparams match {
        case Nil => s"object $cname"
        case ps  => s"class  $cname$ctparams(${cargs.mkString(", ")})"
      }) + s""" extends ${sname}Op[$ret] {
        |      def visit[F[_]](v: Visitor[F]) = v.$mname${if (args.isEmpty) "" else s"($args)"}
        |    }""").trim.stripMargin

    // Argument list: a, b, c, ... up to the proper arity
    def args: String =
      "abcdefghijklmnopqrstuvwxyz".toList.take(cparams.length).mkString(", ")

    // Pattern to match the constructor
    def pat: String =
      cparams match {
        case Nil => s"object $cname"
        case ps  => s"class  $cname(${cargs.mkString(", ")})"
      }

    // Case clause mapping this constructor to the corresponding primitive action
    def prim(sname:String): String =
      (if (cargs.isEmpty)
        s"case $cname => primitive(_.$mname)"
      else
        s"case $cname($args) => primitive(_.$mname($args))")

    // Smart constructor
    def lifted(sname: String): String =
      if (cargs.isEmpty) {
        s"val $mname: ${sname}IO[$ret] = FF.liftF(${cname})"
      } else {
        s"def $mname$ctparams(${cargs.mkString(", ")}): ${sname}IO[$ret] = FF.liftF(${cname}($args))"
      }

    def visitor: String =
      if (cargs.isEmpty) s"|      def $mname: F[$ret]"
      else s"|      def $mname$ctparams(${cargs.mkString(", ")}): F[$ret]"

    def stub: String =
      if (cargs.isEmpty) s"""|      def $mname: F[$ret] = sys.error("Not implemented: $mname")"""
      else s"""|      def $mname$ctparams(${cargs.mkString(", ")}): F[$ret] = sys.error("Not implemented: $mname$ctparams(${cparams.mkString(", ")})")"""

    def kleisliImpl: String =
      if (cargs.isEmpty) s"|    override def $mname = primitive(_.$mname)"
      else s"|    override def $mname$ctparams(${cargs.mkString(", ")}) = primitive(_.$mname($args))"

  }

  // This class, plus any superclasses and interfaces, "all the way up"
  def closure(c: Class[_]): List[Class[_]] =
    (c :: (Option(c.getSuperclass).toList ++ c.getInterfaces.toList).flatMap(closure)).distinct
      .filterNot(_.getName == "java.lang.AutoCloseable") // not available in jdk1.6

  // All method for this class and any superclasses/interfaces
  def methods(c: Class[_]): List[Method] =
    closure(c).flatMap(_.getMethods.toList).distinct

  // Ctor values for all methods in of A plus superclasses, interfaces, etc.
  def ctors[A](implicit ev: ClassTag[A]): List[Ctor] =
    methods(ev.runtimeClass).groupBy(_.getName).toList.flatMap { case (n, ms) =>
      ms.toList.sortBy(_.getGenericParameterTypes.map(toScalaType).mkString(",")).zipWithIndex.map {
        case (m, i) => Ctor(m, i)
      }
    }.sortBy(c => (c.mname, c.index))

  // All types referenced by all methods on A, superclasses, interfaces, etc.
  def imports[A](implicit ev: ClassTag[A]): List[String] =
    (s"import ${ev.runtimeClass.getName}" :: ctors.map(_.method).flatMap { m =>
      m.getReturnType :: managed.toList.filterNot(_ == ev.runtimeClass) ::: m.getParameterTypes.toList
    }.map { t =>
      if (t.isArray) t.getComponentType else t
    }.filterNot(t => t.isPrimitive).map { c =>
      val sn = c.getSimpleName
      val an = renames.getOrElse(c, sn)
      if (sn == an) s"import ${c.getName}"
      else          s"import ${c.getPackage.getName}.{ $sn => $an }"
    }).distinct.sorted




  // The algebra module for A
  def module[A](implicit ev: ClassTag[A]): String = {
    val sname = toScalaType(ev.runtimeClass)
   s"""
    |package doobie.free
    |
    |#+scalaz
    |import doobie.util.capture.Capture
    |import scalaz.{ Catchable, Free => FF, Monad, ~>, \\/ }
    |#-scalaz
    |#+cats
    |import cats.{ Monad, ~> }
    |import cats.free.{ Free => FF }
    |import scala.util.{ Either => \\/ }
    |import fs2.util.{ Catchable, Suspendable }
    |#-cats
    |
    |${imports[A].mkString("\n")}
    |
    |${managed.map(_.getSimpleName).map(c => s"import ${c.toLowerCase}.${c}IO").mkString("\n")}
    |
    |object ${sname.toLowerCase} {
    |
    |  // Algebra of operations for $sname. Each accepts a visitor as an alternatie to pattern-matching.
    |  sealed trait ${sname}Op[A] {
    |    def visit[F[_]](v: ${sname}Op.Visitor[F]): F[A]
    |  }
    |
    |  // Free monad over ${sname}Op.
    |  type ${sname}IO[A] = FF[${sname}Op, A]
    |
    |  // Module of instances and constructors of ${sname}Op.
    |  object ${sname}Op {
    |
    |    // Given a $sname we can embed a ${sname}IO program in any algebra that understands embedding.
    |    implicit val ${sname}OpEmbeddable: Embeddable[${sname}Op, ${sname}] =
    |      new Embeddable[${sname}Op, ${sname}] {
    |        def embed[A](j: ${sname}, fa: FF[${sname}Op, A]) = Embedded.${sname}(j, fa)
    |      }
    |
    |    // Interface for a natural tansformation ${sname}Op ~> F encoded via the visitor pattern.
    |    // This approach is much more efficient than pattern-matching for large algebras.
    |    trait Visitor[F[_]] extends (${sname}Op ~> F) {
    |      final def apply[A](fa: ${sname}Op[A]): F[A] = fa.visit(this)
    |
    |      // Common
    |      def raw[A](f: $sname => A): F[A]
    |      def embed[A](e: Embedded[A]): F[A]
    |      def delay[A](a: () => A): F[A]
    |      def attempt[A](fa: ${sname}IO[A]): F[Throwable \\/ A]
    |
    |      // $sname
          ${ctors[A].map(_.visitor).mkString("\n    ")}
    |
    |    }
    |
    |    // Common operations for all algebras.
    |    case class Raw[A](f: $sname => A) extends ${sname}Op[A] {
    |      def visit[F[_]](v: Visitor[F]) = v.raw(f)
    |    }
    |    case class Embed[A](e: Embedded[A]) extends ${sname}Op[A] {
    |      def visit[F[_]](v: Visitor[F]) = v.embed(e)
    |    }
    |    case class  Delay[A](a: () => A) extends ${sname}Op[A] {
    |      def visit[F[_]](v: Visitor[F]) = v.delay(a)
    |    }
    |    case class  Attempt[A](fa: ${sname}IO[A]) extends ${sname}Op[Throwable \\/ A] {
    |      def visit[F[_]](v: Visitor[F]) = v.attempt(fa)
    |    }
    |
    |    // $sname-specific operations.
    |    ${ctors[A].map(_.ctor(sname)).mkString("\n    ")}
    |
    |  }
    |  import ${sname}Op._
    |
    |  // Smart constructors for operations common to all algebras.
    |  val unit: ${sname}IO[Unit] = FF.pure[${sname}Op, Unit](())
    |  def raw[A](f: $sname => A): ${sname}IO[A] = FF.liftF(Raw(f))
    |  def embed[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[${sname}Op, A] = FF.liftF(Embed(ev.embed(j, fa)))
    |  def lift[F[_], J, A](j: J, fa: FF[F, A])(implicit ev: Embeddable[F, J]): FF[${sname}Op, A] = embed(j, fa)
    |  def delay[A](a: => A): ${sname}IO[A] = FF.liftF(Delay(() => a))
    |  def attempt[A](fa: ${sname}IO[A]): ${sname}IO[Throwable \\/ A] = FF.liftF[${sname}Op, Throwable \\/ A](Attempt(fa))
    |
    |  // Smart constructors for $sname-specific operations.
    |  ${ctors[A].map(_.lifted(sname)).mkString("\n  ")}
    |
    |// ${sname}IO can capture side-effects, and can trap and raise exceptions.
    |#+scalaz
    |  implicit val Catchable${sname}IO: Catchable[${sname}IO] with Capture[${sname}IO] =
    |    new Catchable[${sname}IO] with Capture[${sname}IO] {
    |      def attempt[A](f: ${sname}IO[A]): ${sname}IO[Throwable \\/ A] = ${sname.toLowerCase}.attempt(f)
    |      def fail[A](err: Throwable): ${sname}IO[A] = delay(throw err)
    |      def apply[A](a: => A): ${sname}IO[A] = ${sname.toLowerCase}.delay(a)
    |    }
    |#-scalaz
    |#+fs2
    |  implicit val Catchable${sname}IO: Suspendable[${sname}IO] with Catchable[${sname}IO] =
    |    new Suspendable[${sname}IO] with Catchable[${sname}IO] {
    |      def pure[A](a: A): ${sname}IO[A] = ${sname.toLowerCase}.delay(a)
    |      override def map[A, B](fa: ${sname}IO[A])(f: A => B): ${sname}IO[B] = fa.map(f)
    |      def flatMap[A, B](fa: ${sname}IO[A])(f: A => ${sname}IO[B]): ${sname}IO[B] = fa.flatMap(f)
    |      def suspend[A](fa: => ${sname}IO[A]): ${sname}IO[A] = FF.suspend(fa)
    |      override def delay[A](a: => A): ${sname}IO[A] = ${sname.toLowerCase}.delay(a)
    |      def attempt[A](f: ${sname}IO[A]): ${sname}IO[Throwable \\/ A] = ${sname.toLowerCase}.attempt(f)
    |      def fail[A](err: Throwable): ${sname}IO[A] = delay(throw err)
    |    }
    |#-fs2
    |
    |}
    |""".trim.stripMargin
  }

  def embed[A](implicit ev: ClassTag[A]): String = {
    val sname = toScalaType(ev.runtimeClass)
    s"final case class $sname[A](j: java.sql.$sname, fa: ${sname}IO[A]) extends Embedded[A]"
  }

  // The Embedded definition for all modules.
  def embeds: String =
    s"""
     |package doobie.free
     |
     |#+scalaz
     |import scalaz.Free
     |#-scalaz
     |#+cats
     |import cats.free.Free
     |#-cats
     |
     |${managed.map(_.getSimpleName).map(c => s"import ${c.toLowerCase}.${c}IO").mkString("\n")}
     |
     |// A pair (J, Free[F, A]) with constructors that tie down J and F.
     |sealed trait Embedded[A]
     |object Embedded {
     |  ${managed.map(ClassTag(_)).map(embed(_)).mkString("\n  ") }
     |}
     |
     |// Typeclass for embeddable pairs (J, F)
     |trait Embeddable[F[_], J] {
     |  def embed[A](j: J, fa: Free[F, A]): Embedded[A]
     |}
     |""".trim.stripMargin

   def interp[A](implicit ev: ClassTag[A]): String = {
     val n = toScalaType(ev.runtimeClass)
     s"""
       |  trait ${n}Interpreter extends ${n}Op.Visitor[Kleisli[M, ${n}, ?]] {
       |    // common operations delegate to outer interpeter
       |    override def delay[A](a: () => A): Kleisli[M, ${n}, A] = outer.delay(a)
       |    override def embed[A](e: Embedded[A]): Kleisli[M, ${n}, A] = outer.embed(e)
       |    override def raw[A](f: ${n} => A) = outer.raw(f)
       |    override def attempt[A](fa: ${n}IO[A]) = outer.attempt(fa)(this)
       |    // domain-specific operations are implemented in terms of `primitive`
       |${ctors[A].map(_.kleisliImpl).mkString("\n")}
       |  }
       |""".trim.stripMargin
    }

   // template for a kleisli interpreter
   def kleisliInterpreter: String =
     s"""
      |package doobie.free
      |
      |#+scalaz
      |// Library imports required for the scalaz implementation.
      |import doobie.util.capture.Capture
      |import scalaz.{ Catchable, Free, Kleisli, Monad, ~>, \\/ }
      |#-scalaz
      |#+cats
      |// Library imports required for the Cats implementation.
      |import cats.{ Monad, ~> }
      |import cats.data.Kleisli
      |import cats.free.Free
      |import fs2.util.{ Catchable, Suspendable => Capture }
      |import fs2.interop.cats._
      |import scala.util.{ Either => \\/ }
      |#-cats
      |
      |// Types referenced in the JDBC API
      |${managed.map(ClassTag(_)).flatMap(imports(_)).distinct.sorted.mkString("\n") }
      |
      |// Algebras and free monads thereof referenced by our interpreter.
      |${managed.map(_.getSimpleName).map(c => s"import doobie.free.${c.toLowerCase}.{ ${c}IO, ${c}Op }").mkString("\n")}
      |
      |object KleisliInterpreter {
      |  def apply[M[_]](
      |    implicit M0: Monad[M],
      |             C0: Capture[M],
      |             K0: Catchable[M]
      |  ): KleisliInterpreter[M] =
      |    new KleisliInterpreter[M] {
      |      val M = M0
      |      val C = C0
      |      val K = K0
      |    }
      |}
      |
      |// Family of interpreters into Kleisli arrows for some monad M.
      |trait KleisliInterpreter[M[_]] { outer =>
      |  implicit val M: Monad[M]
      |  implicit val C: Capture[M]
      |  implicit val K: Catchable[M]
      |
      |  // The ${managed.length} interpreters, with definitions below. These can be overridden to customize behavior.
      |  ${managed.map(_.getSimpleName).map(n => s"lazy val ${n}Interpreter: ${n}Op ~> Kleisli[M, ${n}, ?] = new ${n}Interpreter { }").mkString("\n  ")}
      |
      |  // Some methods are common to all interpreters and can be overridden to change behavior globally.
      |  def primitive[J, A](f: J => A): Kleisli[M, J, A] = Kleisli(a => C.delay(f(a)))
      |  def delay[J, A](a: () => A): Kleisli[M, J, A] = primitive(_ => a())
      |  def raw[J, A](f: J => A): Kleisli[M, J, A] = primitive(f)
      |  def attempt[F[_], J, A](fa: Free[F, A])(nat: F ~> Kleisli[M, J, ?]): Kleisli[M, J, Throwable \\/ A] =
      |    Catchable[Kleisli[M, J, ?]].attempt(fa.foldMap(nat))
      |  def embed[J, A](e: Embedded[A]): Kleisli[M, J, A] =
      |    e match {
      |      ${managed.map(_.getSimpleName).map(n => s"case Embedded.${n}(j, fa) => Kleisli(_ => fa.foldMap(${n}Interpreter).run(j))").mkString("\n      ")}
      |    }
      |
      |  // Interpreters
      |${managed.map(ClassTag(_)).map(interp(_)).mkString("\n")}
      |
      |}
      |""".trim.stripMargin

  def gen(base: File): Seq[java.io.File] = {
    import java.io._
    log.info("Generating free algebras into " + base)
    val fs = managed.map { c =>
      base.mkdirs
      val mod  = module(ClassTag(c))
      val file = new File(base, s"${c.getSimpleName.toLowerCase}.scala")
      val pw = new PrintWriter(file)
      pw.println(mod)
      pw.close()
      log.info(s"${c.getName} -> ${file.getName}")
      file
    }
    val e = {
      val file = new File(base, s"embedded.scala")
      val pw = new PrintWriter(file)
      pw.println(embeds)
      pw.close()
      log.info(s"... -> ${file.getName}")
      file
    }
    val ki = {
      val file = new File(base, s"kleisliinterpreter.scala")
      val pw = new PrintWriter(file)
      pw.println(kleisliInterpreter)
      pw.close()
      log.info(s"... -> ${file.getName}")
      file
    }
    ki :: e :: fs
  }

}
