package doobie.free

import java.lang.reflect._

object Gen extends App {

  val clazz = classOf[java.sql.ResultSet]

  val methods = clazz.getMethods.toList.groupBy(_.getName).toList.sortBy { case (a, b) => a + ";" + b}

  // println(s"type ${clazz.getSimpleName}Op[+A] = Free[${clazz.getSimpleName}OpF, A]")

  // Algebra
  println(s"""
  |package doobie.free
  | 
  |import scalaz.{Free, Functor, Monad}, Free._
  |import scalaz.syntax.functor._
  |import scalaz.effect.IO
  |
  |// Free algebra for ${clazz.getName} (auto-generated)
  |sealed abstract class ${clazz.getSimpleName}OpF[+A]
  |object ${clazz.getSimpleName}OpF {
  |
  |  // We can't implement a transformer so MonadCatchIO is implemented as primitives
  |  final case class LiftIO[A](io: IO[A]) extends ${clazz.getSimpleName}OpF[A]
  |  final case class Except[A](fa: ${clazz.getSimpleName}OpF[A], f: Throwable => ${clazz.getSimpleName}OpF[A]) extends ${clazz.getSimpleName}OpF[A]
  |
  |  // Constructors""".stripMargin)
  for {
    (n, ms) <- methods
    (m, i)  <- ms.zipWithIndex
  } gen(n, i, m)

  println(s"""
  |
  |  // Functor instance
  |  implicit def functor${clazz.getSimpleName}OpF: Functor[${clazz.getSimpleName}OpF] =
  |    new Functor[${clazz.getSimpleName}OpF] {
  |      def map[A,B](fa: ${clazz.getSimpleName}OpF[A])(f: A => B): ${clazz.getSimpleName}OpF[B] =
  |        fa match {
  |        
  |          // MonadCatchIO
  |          case LiftIO(a) => LiftIO(a map f)
  |          case Except(a, g) => Except(a map f, t => g(t) map f)
  |
  |          // Standard Constructors
  """.trim.stripMargin)
  for {
    (n, ms) <- methods
    (m, i)  <- ms.zipWithIndex
  } fcase(n, i, m)
  println(s"""
  |        }
  |    }
  |
  |}
  |
  |
  """.trim.stripMargin)

  def gen(n: String, i: Int, m: Method): Unit = {
    val uname = (n.head.toUpper + n.tail) + (if (i == 0) "" else i.toString)
    //todo: tparams
    val params = for {
      (t, i) <- m.getGenericParameterTypes.toList.zipWithIndex
    } yield s"p$i: ${scalaType(t)}"
    val ret = scalaType(m.getGenericReturnType)
    println(f"  final case class $uname[A](${(params :+ s"k: $ret => A").mkString(", ")}) extends ${clazz.getSimpleName}OpF[A]")
    // println(f"def $n(${params.mkString(", ")}): ${clazz.getSimpleName}Op[$ret] = $uname(${(0 to params.length).map(n => s"p$n").mkString(", ")}, identity)")
    // println(s"case $uname(${(0 to params.length).map(n => s"p$n").mkString(", ")}, f) => k(s.$n(${(0 to params.length).map(n => s"p$n").mkString(", ")})")
  }

 def fcase(n: String, i: Int, m: Method): Unit = {
    val uname = (n.head.toUpper + n.tail) + (if (i == 0) "" else i.toString)
    //todo: tparams
    val params = for {
      (t, i) <- m.getGenericParameterTypes.toList.zipWithIndex
    } yield s"p$i: ${scalaType(t)}"
    val ret = scalaType(m.getGenericReturnType)
    println(s"          case $uname(${((0 until params.length).map(n => s"p$n") :+ "k").mkString(", ")}) => $uname(${((0 until params.length).map(n => s"p$n") :+ "f compose k").mkString(", ")})")
  }

  // Ctors
  println(s"""
  |// Client module
  |object ${clazz.getSimpleName.toLowerCase} {
  |  import ${clazz.getSimpleName}OpF._
  |
  |  type ${clazz.getSimpleName}Op[+A] = Free[${clazz.getSimpleName}OpF, A]
  |  Monad[${clazz.getSimpleName}Op] // proof that we have a monad now
  |
  |  // Client constructors, 1:1 with ${clazz.getName} (including overloading)
  """.trim.stripMargin)
  for {
    (n, ms) <- methods
    (m, i)  <- ms.zipWithIndex
  } cons(n, i, m)
  println(s"""
  |
  |  // Interpreter
  |  def unsafeRun${clazz.getSimpleName}[A](s: ${clazz.getSimpleName}, a: ${clazz.getSimpleName}Op[A]): A =
  |    a.go {
  |
  |      // MonadCatchIO
  |      //case LiftIO(a) => Suspend(s.unsafePerformIO)
  |      //case Except(a, f) => Suspend(try a.unsafeRun(s, a) catch f)
  |
  |      // Standard Constructors
  """.trim.stripMargin)
  for {
    (n, ms) <- methods
    (m, i)  <- ms.zipWithIndex
  } interp(n, i, m)
  println("""
  |
  |    }
  |
  |}
  """.trim.stripMargin)
  println()

 def cons(n: String, i: Int, m: Method): Unit = {
    val uname = (n.head.toUpper + n.tail) + (if (i == 0) "" else i.toString)
    //todo: tparams
    val params = for {
      (t, i) <- m.getGenericParameterTypes.toList.zipWithIndex
    } yield s"p$i: ${scalaType(t)}"
    val ret = scalaType(m.getGenericReturnType)
    println(f"  def $n(${params.mkString(", ")}): ${clazz.getSimpleName}Op[$ret] = Suspend($uname(${((0 until params.length).map(n => s"p$n") :+ "Return(_)").mkString(", ")}))".replaceAll("""\(\)""", ""))
    // println(s"case $uname(${(0 to params.length).map(n => s"p$n").mkString(", ")}, f) => k(s.$n(${(0 to params.length).map(n => s"p$n").mkString(", ")})")
  }

 def interp(n: String, i: Int, m: Method): Unit = {
    val uname = (n.head.toUpper + n.tail) + (if (i == 0) "" else i.toString)
    //todo: tparams
    val params = for {
      (t, i) <- m.getGenericParameterTypes.toList.zipWithIndex
    } yield s"p$i: ${scalaType(t)}"
    val ret = scalaType(m.getGenericReturnType)
    println(s"      case $uname(${((0 until params.length).map(n => s"p$n") :+ "k").mkString(", ")}) => k(s.$n(${(0 until params.length).map(n => s"p$n").mkString(", ")}))")
  }


  def scalaType(t: Type): String = // TODO: accumulate imports
    t match {
      case a: GenericArrayType => s"Array[${scalaType(a.getGenericComponentType)}]"
      case p: ParameterizedType => s"${scalaType(p.getRawType)}[${p.getActualTypeArguments.map(scalaType).mkString(", ")}]"
      case c: Class[_] if c.isArray => s"Array[${scalaClass(c.getComponentType)}]"
      case c: Class[_] => scalaClass(c)
      case w: WildcardType => "_" // TODO: bounds
      case v: TypeVariable[_] => v.getName

      case x => sys.error("Unexpected type: " + x)
    }

  def scalaClass(c: Class[_]): String =
    if (c == Void.TYPE) {
      "Unit"
    } else if (c.isPrimitive) {
      val n = c.getName
      n.head.toUpper + n.tail
    } else {
      c.getName // TODO: parent type (needed?)
    }

}

