import sbt._, Keys._
import java.lang.reflect._
import scala.reflect.ClassTag
import Predef._

object FreeGen {

  lazy val freeGenClasses = settingKey[List[Class[_]]]("classes for which free algebras should be generated")
  lazy val freeGenDir = settingKey[File]("directory where free algebras go")
  lazy val freeGen = taskKey[Seq[File]]("generate free algebras")

  lazy val freeGenSettings = Seq(
    freeGenClasses := Nil,
    freeGenDir := (sourceManaged in Compile).value,
    freeGen := new FreeGen(freeGenClasses.value, state.value.log).gen(freeGenDir.value)
  )

}

class FreeGen(managed: List[Class[_]], log: Logger) {

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
    def ctor(constraints: String, sname:String): String =
      ("|case " + (cparams match {
        case Nil => s"object $cname"
        case ps  => s"class  $cname$ctparams(${cargs.mkString(", ")})"
      }) + s""" extends ${sname}Op[$ret] {
        |      override def defaultTransK[M[_]: $constraints] = primitive(_.$mname($args))
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
        s"""|/**
            |   * @group Constructors (Primitives)
            |   */
            |  val $mname: ${sname}IO[$ret] =
            |    F.liftF(${cname})
         """.trim.stripMargin
      } else {
        s"""|/**
            |   * @group Constructors (Primitives)
            |   */
            |  def $mname$ctparams(${cargs.mkString(", ")}): ${sname}IO[$ret] =
            |    F.liftF(${cname}($args))
         """.trim.stripMargin
      }

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
    |import scalaz.{ Catchable, Free => F, Kleisli, Monad, ~>, \\/ }
    |#-scalaz
    |#+cats
    |import cats.~>
    |import cats.data.Kleisli
    |import cats.free.{ Free => F }
    |import scala.util.{ Either => \\/ }
    |#-cats
    |#+fs2
    |import fs2.util.{ Catchable, Suspendable }
    |import fs2.interop.cats._
    |#-fs2
    |
    |import doobie.util.capture._
    |import doobie.free.kleislitrans._
    |
    |${imports[A].mkString("\n")}
    |
    |${managed.map(_.getSimpleName).map(c => s"import ${c.toLowerCase}.${c}IO").mkString("\n")}
    |
    |/**
    | * Algebra and free monad for primitive operations over a `${ev.runtimeClass.getName}`. This is
    | * a low-level API that exposes lifecycle-managed JDBC objects directly and is intended mainly
    | * for library developers. End users will prefer a safer, higher-level API such as that provided
    | * in the `doobie.hi` package.
    | *
    | * `${sname}IO` is a free monad that must be run via an interpreter, most commonly via
    | * natural transformation of its underlying algebra `${sname}Op` to another monad via
    | * `Free#foldMap`.
    | *
    | * The library provides a natural transformation to `Kleisli[M, ${sname}, A]` for any
    | * exception-trapping (`Catchable`) and effect-capturing (`Capture`) monad `M`. Such evidence is
    | * provided for `Task`, `IO`, and stdlib `Future`; and `transK[M]` is provided as syntax.
    | *
    | * {{{
    | * // An action to run
    | * val a: ${sname}IO[Foo] = ...
    | *
    | * // A JDBC object
    | * val s: ${sname} = ...
    | *
    | * // Unfolding into a Task
    | * val ta: Task[A] = a.transK[Task].run(s)
    | * }}}
    | *
    | * @group Modules
    | */
    |object ${sname.toLowerCase} extends ${sname}IOInstances {
    |
    |  /**
    |   * Sum type of primitive operations over a `${ev.runtimeClass.getName}`.
    |   * @group Algebra
    |   */
    |  sealed trait ${sname}Op[A] {
    |#+scalaz
    |    protected def primitive[M[_]: Monad: Capture](f: ${sname} => A): Kleisli[M, ${sname}, A] =
    |      Kleisli((s: ${sname}) => Capture[M].apply(f(s)))
    |    def defaultTransK[M[_]: Monad: Catchable: Capture]: Kleisli[M, ${sname}, A]
    |#-scalaz
    |#+fs2
    |    protected def primitive[M[_]: Suspendable](f: ${sname} => A): Kleisli[M, ${sname}, A] =
    |      Kleisli((s: ${sname}) => Predef.implicitly[Suspendable[M]].delay(f(s)))
    |    def defaultTransK[M[_]: Catchable: Suspendable]: Kleisli[M, ${sname}, A]
    |#-fs2
    |  }
    |
    |  /**
    |   * Module of constructors for `${sname}Op`. These are rarely useful outside of the implementation;
    |   * prefer the smart constructors provided by the `${sname.toLowerCase}` module.
    |   * @group Algebra
    |   */
    |  object ${sname}Op {
    |
    |    // This algebra has a default interpreter
    |    implicit val ${sname}KleisliTrans: KleisliTrans.Aux[${sname}Op, ${sname}] =
    |      new KleisliTrans[${sname}Op] {
    |        type J = ${sname}
    |#+scalaz
    |        def interpK[M[_]: Monad: Catchable: Capture]: ${sname}Op ~> Kleisli[M, ${sname}, ?] =
    |#-scalaz
    |#+fs2
    |        def interpK[M[_]: Catchable: Suspendable]: ${sname}Op ~> Kleisli[M, ${sname}, ?] =
    |#-fs2
    |          new (${sname}Op ~> Kleisli[M, ${sname}, ?]) {
    |            def apply[A](op: ${sname}Op[A]): Kleisli[M, ${sname}, A] =
    |              op.defaultTransK[M]
    |          }
    |      }
    |
    |    // Lifting
    |    case class Lift[Op[_], A, J](j: J, action: F[Op, A], mod: KleisliTrans.Aux[Op, J]) extends ${sname}Op[A] {
    |#+scalaz
    |      override def defaultTransK[M[_]: Monad: Catchable: Capture] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    |#-scalaz
    |#+fs2
    |      override def defaultTransK[M[_]: Catchable: Suspendable] = Kleisli(_ => mod.transK[M].apply(action).run(j))
    |#-fs2
    |    }
    |
    |    // Combinators
    |    case class Attempt[A](action: ${sname}IO[A]) extends ${sname}Op[Throwable \\/ A] {
    |#+scalaz
    |      override def defaultTransK[M[_]: Monad: Catchable: Capture] =
    |#-scalaz
    |#+fs2
    |      override def defaultTransK[M[_]: Catchable: Suspendable] =
    |#-fs2
    |        Predef.implicitly[Catchable[Kleisli[M, ${sname}, ?]]].attempt(action.transK[M])
    |    }
    |    case class Pure[A](a: () => A) extends ${sname}Op[A] {
    |#+scalaz
    |      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(_ => a())
    |#-scalaz
    |#+fs2
    |      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(_ => a())
    |#-fs2
    |    }
    |    case class Raw[A](f: ${sname} => A) extends ${sname}Op[A] {
    |#+scalaz
    |      override def defaultTransK[M[_]: Monad: Catchable: Capture] = primitive(f)
    |#-scalaz
    |#+fs2
    |      override def defaultTransK[M[_]: Catchable: Suspendable] = primitive(f)
    |#-fs2
    |    }
    |
    |    // Primitive Operations
    |#+scalaz
    |    ${ctors[A].map(_.ctor("Monad: Catchable: Capture", sname)).mkString("\n    ")}
    |#-scalaz
    |#+fs2
    |    ${ctors[A].map(_.ctor("Catchable: Suspendable", sname)).mkString("\n    ")}
    |#-fs2
    |
    |  }
    |  import ${sname}Op._ // We use these immediately
    |
    |  /**
    |   * Free monad over a free functor of [[${sname}Op]]; abstractly, a computation that consumes
    |   * a `${ev.runtimeClass.getName}` and produces a value of type `A`.
    |   * @group Algebra
    |   */
    |  type ${sname}IO[A] = F[${sname}Op, A]
    |
    |  /**
    |   * Catchable instance for [[${sname}IO]].
    |   * @group Typeclass Instances
    |   */
    |  implicit val Catchable${sname}IO: Catchable[${sname}IO] =
    |    new Catchable[${sname}IO] {
    |#+fs2
    |      def pure[A](a: A): ${sname}IO[A] = ${sname.toLowerCase}.delay(a)
    |      override def map[A, B](fa: ${sname}IO[A])(f: A => B): ${sname}IO[B] = fa.map(f)
    |      def flatMap[A, B](fa: ${sname}IO[A])(f: A => ${sname}IO[B]): ${sname}IO[B] = fa.flatMap(f)
    |#-fs2
    |      def attempt[A](f: ${sname}IO[A]): ${sname}IO[Throwable \\/ A] = ${sname.toLowerCase}.attempt(f)
    |      def fail[A](err: Throwable): ${sname}IO[A] = ${sname.toLowerCase}.delay(throw err)
    |    }
    |
    |#+scalaz
    |  /**
    |   * Capture instance for [[${sname}IO]].
    |   * @group Typeclass Instances
    |   */
    |  implicit val Capture${sname}IO: Capture[${sname}IO] =
    |    new Capture[${sname}IO] {
    |      def apply[A](a: => A): ${sname}IO[A] = ${sname.toLowerCase}.delay(a)
    |    }
    |#-scalaz
    |
    |  /**
    |   * Lift a different type of program that has a default Kleisli interpreter.
    |   * @group Constructors (Lifting)
    |   */
    |  def lift[Op[_], A, J](j: J, action: F[Op, A])(implicit mod: KleisliTrans.Aux[Op, J]): ${sname}IO[A] =
    |    F.liftF[${sname}Op, A](Lift(j, action, mod))
    |
    |  /**
    |   * Lift a ${sname}IO[A] into an exception-capturing ${sname}IO[Throwable \\/ A].
    |   * @group Constructors (Lifting)
    |   */
    |  def attempt[A](a: ${sname}IO[A]): ${sname}IO[Throwable \\/ A] =
    |    F.liftF[${sname}Op, Throwable \\/ A](Attempt(a))
    |
    |  /**
    |   * Non-strict unit for capturing effects.
    |   * @group Constructors (Lifting)
    |   */
    |  def delay[A](a: => A): ${sname}IO[A] =
    |    F.liftF(Pure(a _))
    |
    |  /**
    |   * Backdoor for arbitrary computations on the underlying ${sname}.
    |   * @group Constructors (Lifting)
    |   */
    |  def raw[A](f: ${sname} => A): ${sname}IO[A] =
    |    F.liftF(Raw(f))
    |
    |  ${ctors[A].map(_.lifted(sname)).mkString("\n\n  ")}
    |
    | /**
    |  * Natural transformation from `${sname}Op` to `Kleisli` for the given `M`, consuming a `${ev.runtimeClass.getName}`.
    |  * @group Algebra
    |  */
    |#+scalaz
    |  def interpK[M[_]: Monad: Catchable: Capture]: ${sname}Op ~> Kleisli[M, ${sname}, ?] =
    |   ${sname}Op.${sname}KleisliTrans.interpK
    |#-scalaz
    |#+fs2
    |  def interpK[M[_]: Catchable: Suspendable]: ${sname}Op ~> Kleisli[M, ${sname}, ?] =
    |   ${sname}Op.${sname}KleisliTrans.interpK
    |#-fs2
    |
    | /**
    |  * Natural transformation from `${sname}IO` to `Kleisli` for the given `M`, consuming a `${ev.runtimeClass.getName}`.
    |  * @group Algebra
    |  */
    |#+scalaz
    |  def transK[M[_]: Monad: Catchable: Capture]: ${sname}IO ~> Kleisli[M, ${sname}, ?] =
    |   ${sname}Op.${sname}KleisliTrans.transK
    |#-scalaz
    |#+fs2
    |  def transK[M[_]: Catchable: Suspendable]: ${sname}IO ~> Kleisli[M, ${sname}, ?] =
    |   ${sname}Op.${sname}KleisliTrans.transK
    |#-fs2
    |
    | /**
    |  * Natural transformation from `${sname}IO` to `M`, given a `${ev.runtimeClass.getName}`.
    |  * @group Algebra
    |  */
    |#+scalaz
    | def trans[M[_]: Monad: Catchable: Capture](c: $sname): ${sname}IO ~> M =
    |#-scalaz
    |#+fs2
    | def trans[M[_]: Catchable: Suspendable](c: $sname): ${sname}IO ~> M =
    |#-fs2
    |   ${sname}Op.${sname}KleisliTrans.trans[M](c)
    |
    |  /**
    |   * Syntax for `${sname}IO`.
    |   * @group Algebra
    |   */
    |  implicit class ${sname}IOOps[A](ma: ${sname}IO[A]) {
    |#+scalaz
    |    def transK[M[_]: Monad: Catchable: Capture]: Kleisli[M, ${sname}, A] =
    |#-scalaz
    |#+fs2
    |    def transK[M[_]: Catchable: Suspendable]: Kleisli[M, ${sname}, A] =
    |#-fs2
    |      ${sname}Op.${sname}KleisliTrans.transK[M].apply(ma)
    |  }
    |
    |}
    |
    |private[free] trait ${sname}IOInstances {
    |#+fs2
    |  /**
    |   * Suspendable instance for [[${sname}IO]].
    |   * @group Typeclass Instances
    |   */
    |  implicit val Suspendable${sname}IO: Suspendable[${sname}IO] =
    |    new Suspendable[${sname}IO] {
    |      def pure[A](a: A): ${sname}IO[A] = ${sname.toLowerCase}.delay(a)
    |      override def map[A, B](fa: ${sname}IO[A])(f: A => B): ${sname}IO[B] = fa.map(f)
    |      def flatMap[A, B](fa: ${sname}IO[A])(f: A => ${sname}IO[B]): ${sname}IO[B] = fa.flatMap(f)
    |      def suspend[A](fa: => ${sname}IO[A]): ${sname}IO[A] = F.suspend(fa)
    |      override def delay[A](a: => A): ${sname}IO[A] = ${sname.toLowerCase}.delay(a)
    |    }
    |#-fs2
    |}
    |""".trim.stripMargin
  }

  def gen(base: File): Seq[java.io.File] = {
    import java.io._
    log.info("Generating free algebras into " + base)
    managed.map { c =>
      base.mkdirs
      val mod  = module(ClassTag(c))
      val file = new File(base, s"${c.getSimpleName.toLowerCase}.scala")
      val pw = new PrintWriter(file)
      pw.println(mod)
      pw.close()
      log.info(s"${c.getName} -> ${file.getName}")
      file
    }
  }

}


