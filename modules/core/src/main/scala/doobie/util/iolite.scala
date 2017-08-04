package doobie.util

import scala.annotation.tailrec
import scala.language.higherKinds

import scalaz.{ Monad, Catchable, \/, -\/, \/- }
import doobie.util.capture.Capture

object iolite {
  sealed abstract class IOLite[A] { self =>

    def unsafePerformIO: A

    def map[B](f: A => B): IOLite[B] =
      flatMap(a => IOLite.Pure(f(a)))

    def flatMap[B](f: A => IOLite[B]): IOLite[B] =
      this match {
        case c: IOLite.Compute[A] =>
          new IOLite.Compute[B] {
            type Start = c.Start
            // https://issues.scala-lang.org/browse/SI-9931
            val start: () => IOLite[Start] = c.start
            val run: Start => IOLite[B] = (s: c.Start) =>
              new IOLite.Compute[B] {
                type Start = A
                val start = () => c.run(s)
                val run = f
              }
          }
        case _ =>
          new IOLite.Compute[B] {
            type Start = A
            val start = () => self
            val run = f
          }
      }

    def attempt: IOLite[Throwable \/ A] =
      IOLite.primitive(try \/-(unsafePerformIO) catch { case t: Throwable => -\/(t) })

    def void: IOLite[Unit] =
      map(_ => ())

  }

  object IOLite extends IOInstances with IOFunctions {

    def pure[A](a: A): IOLite[A] =
      Pure(a)

    def primitive[A](a: => A): IOLite[A] =
      new Primitive(a _)

    private final case class Pure[A](unsafePerformIO: A) extends IOLite[A]

    private final class Primitive[A](f: () => A) extends IOLite[A] {
      def unsafePerformIO: A = f()
    }

    private sealed abstract class Compute[A] extends IOLite[A] {
      type Start
      val start: () => IOLite[Start]
      val run: Start => IOLite[A]
      def unsafePerformIO: A = {
        type L = IOLite[Any]
        type C = Any => IOLite[Any]
        @tailrec def loop(curr: L, fs: List[C]): Any =
          curr match {
            case c: Compute[_] =>
              c.start() match {
                case cc: Compute[_] =>
                  loop(
                    cc.start().asInstanceOf[L],
                    cc.run.asInstanceOf[C] :: c.run.asInstanceOf[C] :: fs)
                case xx =>
                  loop(c.run(xx.unsafePerformIO).asInstanceOf[L], fs)
              }
            case x =>
              fs match {
                case f :: fs => loop(f(x.unsafePerformIO), fs)
                case Nil => x.unsafePerformIO
              }
          }
        loop(this.asInstanceOf[L], Nil).asInstanceOf[A]
      }
    }

  }

  trait IOInstances extends IOInstances0 {

    implicit val MonadIOLite: Monad[IOLite] =
      new Monad[IOLite] {
        def bind[A, B](fa: IOLite[A])(f: A => IOLite[B]): IOLite[B] = fa.flatMap(f)
        def point[A](a: => A): IOLite[A] = IOLite.pure(a)
      }

    implicit val CaptureIOLite: Capture[IOLite] =
      new Capture[IOLite] {
        def apply[A](a: => A) = IOLite.primitive(a)
      }

    implicit val CatchableIOLite: Catchable[IOLite] =
      new Catchable[IOLite] {
        def attempt[A](fa: IOLite[A]): IOLite[Throwable \/ A] = fa.attempt
        def fail[A](t: Throwable): IOLite[A] = IOLite.fail(t)
      }
  }

  private[iolite] trait IOInstances0 {
  }

  trait IOFunctions {

    val unit: IOLite[Unit] =
      IOLite.pure(())

    def fail[A](t: Throwable): IOLite[A] =
      IOLite.primitive(throw t)

  }

}
