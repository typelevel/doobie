#+cats
package doobie.example

import cats.~>
import cats.data.{ Coproduct, Kleisli }
import cats.free.{ Free, Inject }
import cats.implicits._
import fs2.interop.cats._

import doobie.free.connection.ConnectionOp
import doobie.imports._

import java.sql.Connection
import scala.io.StdIn

object coproduct {

  // This is merged in cats
  implicit class MoreFreeOps[F[_], A](fa: Free[F, A]) {
    def inject[G[_]](implicit ev: Inject[F, G]): Free[G, A] =
      fa.compile(λ[F ~> G](ev.inj(_)))
  }

  // This is kind of eh … we need to interpret into Kleisli so this is helpful
  implicit class MoreNaturalTransformationOps[F[_], G[_]](nat: F ~> G) {
    def liftK[E] = λ[F ~> Kleisli[G, E, ?]](fa => Kleisli(_ => nat(fa)))
  }

  // A console algebra
  sealed trait ConsoleOp[A]
  case object ReadLn             extends ConsoleOp[String]
  case class  PrintLn(s: String) extends ConsoleOp[Unit]

  // A module of ConsoleOp constructors, parameterized over a coproduct
  class ConsoleOps[F[_]](implicit ev: Inject[ConsoleOp, F]) {
    val readLn             = Free.inject[ConsoleOp, F](ReadLn)
    def printLn(s: String) = Free.inject[ConsoleOp, F](PrintLn(s))
  }
  object ConsoleOps {
    implicit def instance[F[_]](implicit ev: Inject[ConsoleOp, F]) = new ConsoleOps
  }

  // An interpreter into IOLite
  val consoleInterp = λ[ConsoleOp ~> IOLite] {
    case ReadLn     => IOLite.primitive(StdIn.readLine)
    case PrintLn(s) => IOLite.primitive(Console.println(s))
  }

  // A module of ConnectionOp programs, parameterized over a coproduct. The trick here is that these
  // are domain-specific operations that are injected as programs, not as constructors (which would
  // work but is too low-level to be useful).
  class ConnectionOps[F[_]](implicit ev: Inject[ConnectionOp, F]) {
    def select(pat: String): Free[F, List[String]] =
      sql"select name from country where name like $pat".query[String].list.inject[F]
  }
  object ConnectionOps {
    implicit def instance[F[_]](implicit ev: Inject[ConnectionOp, F]) = new ConnectionOps
  }

  // A program
  def prog[F[_]](implicit ev1: ConsoleOps[F], ev2: ConnectionOps[F]): Free[F, Unit] = {
    import ev1._, ev2._
    for {
      _   <- printLn("Enter a pattern:")
      pat <- readLn
      ns  <- select(pat)
      _   <- ns.traverse(printLn)
    } yield ()
  }

  // Our coproduct
  type Cop[A] = Coproduct[ConsoleOp, ConnectionOp, A]

  // Our interpreter must be parameterized over a connection so we can add transaction boundaries
  // before and after.
  val interp: Cop ~> Kleisli[IOLite, Connection, ?] =
    consoleInterp.liftK[Connection] or KleisliInterpreter[IOLite].ConnectionInterpreter

  // Our interpreted program
  val iprog: Kleisli[IOLite, Connection, Unit] = prog[Cop].foldMap(interp)

  // Exec it!
  def main(args: Array[String]): Unit = {
    val xa = DriverManagerTransactor[IOLite](
      "org.postgresql.Driver",
      "jdbc:postgresql:world",
      "postgres", ""
    )
    xa.exec.apply(iprog).unsafePerformIO
  }

  // Enter a pattern:
  // U%
  // United Arab Emirates
  // United Kingdom
  // Uganda
  // Ukraine
  // Uruguay
  // Uzbekistan
  // United States
  // United States Minor Outlying Islands

}

#-cats
