package doobie
package dbc

import scala.language.higherKinds
import scalaz.effect.{ IO, MonadCatchIO }
import scalaz.syntax.effect.monadCatchIO._
import scalaz._
import Scalaz._

/*

  IDEAS -

    should be Log[E: Json] {
      def log[M[+_]: MonadCatchIO, A: Json](e: => E, ma: M[A]): M[A]
    }

  for two reasons:
    (1) we can serializer
    (2) it prevents us from hanging onto anything voliatile like a connection because you can't 
        define Json for it.

 */


/** Effectful logger producing a value of type `L`. */
trait Log[E] {
  def log[M[+_]: MonadCatchIO, A](e: => E, ma: M[A]): M[A]
  def dump: IO[Unit]
}

object Log {

  def consoleLog(rootLabel: String): IO[Log[String]] = IO {
    new Log[String] {
      import IO._

      var z: TreeLoc[String] =
        Tree(s"${Console.BOLD}$rootLabel${Console.RESET}").loc

      def out(s: String, a: Any, t: Long): IO[Unit] = IO {
        val label = f"${s.take(80)}%-80s|  ${a.toString.take(40)}%-40s  ${t/1000000.0}%9.3f ms".replaceAll("\n", "/")
        z = z.setLabel(label).parent.get
      }

      def ok(t: Long, s: String, a: Any): IO[Unit] =
        for {
          d <- IO(System.nanoTime - t)
          _ <- out(s"${Console.GREEN}[ok]${Console.RESET} $s", a, d)
        } yield ()

      def error[A](t: Long, s: String, e: Throwable): IO[A] =
        for {
          d <- IO(System.nanoTime - t)
          _ <- out(s"${Console.RED}[ex]${Console.RESET} $s", e.getMessage, d)
        } yield (throw e)

      def log[M[+_]: MonadCatchIO, A](s: => String, ma: M[A]): M[A] =
        for {
          t <- IO(System.nanoTime).liftIO[M]
          _ <- IO(z = z.insertDownLast(Tree("pending"))).liftIO[M]
          a <- ma.except(error[A](t, s, _).liftIO[M])
          _ <- ok(t, s, a).liftIO[M]
        } yield a

      def dump: IO[Unit] = IO {
        val rows = z.toTree.draw(Show.showA).zipWithIndex.filter(_._2 % 2 == 0).map(_._1)
        val pipe = rows.map(_.lastIndexOf("|")).max
        val done = rows.map { s =>
          val (l, r) = s.splitAt(s.lastIndexOf('|'))
          l.isEmpty ? s | l.padTo(pipe, ' ') + r
        }
        println()
        done.foreach(s => println("  " + s))
        println()
      }

    }
  }

}
