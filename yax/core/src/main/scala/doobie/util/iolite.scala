package doobie.util

import scala.annotation.tailrec
import scala.language.higherKinds

#+scalaz
import scalaz.{ Monad, Catchable, \/, -\/, \/- }
import doobie.util.capture.Capture
#-scalaz
#+cats
import scala.util.{ Either => \/ }
import scala.util.{ Left => -\/, Right => \/- }
#-cats
#+fs2
import fs2.util.{ Catchable, Suspendable, Monad }
#-fs2

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
            val start = c.start
            val run = (s: c.Start) =>
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

#+scalaz
    implicit val MonadIOLite: Monad[IOLite] = 
      new Monad[IOLite] {
        def bind[A, B](fa: IOLite[A])(f: A => IOLite[B]): IOLite[B] = fa.flatMap(f)
        def point[A](a: => A): IOLite[A] = IOLite.pure(a)
      }

    implicit val CaptureIOLite: Capture[IOLite] =
      new Capture[IOLite] {
        def apply[A](a: => A) = IOLite.primitive(a)
      }
#-scalaz

    implicit val CatchableIOLite: Catchable[IOLite] =
      new Catchable[IOLite] {
#+fs2
        def pure[A](a: A): IOLite[A] = IOLite.pure(a)
        override def map[A, B](ma: IOLite[A])(f: A => B): IOLite[B] = ma.map(f)
        def flatMap[A, B](ma: IOLite[A])(f: A => IOLite[B]): IOLite[B] = ma.flatMap(f)
#-fs2
        def attempt[A](fa: IOLite[A]): IOLite[Throwable \/ A] = fa.attempt
        def fail[A](t: Throwable): IOLite[A] = IOLite.fail(t)
      }
  }

  private[iolite] trait IOInstances0 {
#+fs2
    implicit val SuspendableIOLite: Suspendable[IOLite] =
      new Suspendable[IOLite] {
        def pure[A](a: A): IOLite[A] = IOLite.pure(a)
        override def map[A, B](ma: IOLite[A])(f: A => B): IOLite[B] = ma.map(f)
        def flatMap[A, B](ma: IOLite[A])(f: A => IOLite[B]): IOLite[B] = ma.flatMap(f)
        def suspend[A](ma: => IOLite[A]): IOLite[A] = IOLite.unit.flatMap(_ => ma)
      }
#-fs2
  }

  trait IOFunctions {

    val unit: IOLite[Unit] =
      IOLite.pure(())

    def fail[A](t: Throwable): IOLite[A] =
      IOLite.primitive(throw t)

  }

}
