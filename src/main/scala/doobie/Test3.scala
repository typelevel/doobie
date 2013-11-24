package doobie

import scalaz._
import scalaz.effect._
import Scalaz._

object DBMonad extends App {

  case class Failure(t: Throwable, log: Log)

  type Log = List[String]

  type DB[S,+A] = StateT[({type λ[+α] = EitherT[IO, Failure, α]})#λ, (S, Log), A]

  type Answer[+A] = EitherT[IO, Failure, A]

  trait DBOps[S] {

    type DBS[+A] = DB[S,A]

    // State operations over the left side
    def get: DBS[S] = 
      State.get[(S,Log)].map(_._1).lift[Answer]

    def gets[A](f: S => A): DBS[A] = 
      State.gets[(S,Log), A](_.leftMap(f)._1).lift[Answer]

    def modify(f: S => S): DBS[Unit] = 
      State.modify[(S, Log)](_.leftMap(f)).lift[Answer]

    def put(s: S): DBS[Unit] = 
      State.modify[(S, Log)](_.leftMap(_ => s)).lift[Answer]

    // Log (TODO)
    def log(s:String): DBS[Unit] =
      State.modify[(S, Log)](_.rightMap(s :: _)).lift[Answer]

    // Unit
    def unit[A](a: => A): DBS[A] = 
      State.state(a).lift[Answer]

    // Fail
    def halt[A](t: Throwable): DBS[A] =
      StateT[Answer, (S, Log), A](s => EitherT(IO(-\/(Failure(t, s._2)))))

    // MonadIO instance for Op
    implicit def monadIO = new MonadIO[({type l[a] = DBS[a]})#l] {
      def point[A](a: => A): DBS[A] = unit(a)
      def bind[A, B](fa: DBS[A])(f: A => DBS[B]): DBS[B] = fa.flatMap(f)
      def liftIO[A](ioa: IO[A]): DBS[A] = StateT[Answer, (S, Log), A] { s =>
        EitherT(ioa.map(a => (s, a)).catchLeft.map(_.leftMap(t => Failure(t, s._2)))) // ! this traps our exceptions
      }
    }

    // prove we have monads
    def monad = Monad[DBS]
    def monadio = MonadIO[DBS]

    implicit class Ops[A](a: DBS[A]) {
      def :++>(s: String): DBS[A] = 
        log(s) >> a
    }

  }

  object StringDB extends DBOps[String]
  import StringDB._

  val a = 
    for {
      s <- unit("abc")                                    :++> "lifting abc"
      _ <- modify(_ + "xxx")                              :++> "modifying"
      _ <- IO.putStrLn("hello").liftIO[DBS]               :++> "saying hi!"
      _ <- IO(if (false) throw new Exception).liftIO[DBS] :++> "possibly throwing"
      _ <- unit(())                                       :++> "shouldn't get here"
    } yield "woo"


  println(a.run(("bar", Nil)).run.unsafePerformIO)

}

