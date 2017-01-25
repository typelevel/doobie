package doobie.util


import doobie.free.connection.{ ConnectionIO, setAutoCommit, commit, rollback, close, delay }
import doobie.free.KleisliInterpreter
import doobie.syntax.catchable.ToDoobieCatchableOps._
import doobie.syntax.process._
import doobie.util.query._
import doobie.util.update._
#+cats
import doobie.util.catchable._
#-cats
import doobie.util.yolo._
import doobie.util.naturaltransformation._

#+scalaz
import scalaz.syntax.monad._
import scalaz.stream.Process
import scalaz.stream.Process. { eval, eval_, halt }
import scalaz.{ Monad, Catchable, Kleisli, ~> }
import scalaz.stream.Process
import doobie.util.capture._
#-scalaz
#+cats
import cats.~>
import cats.implicits._
#-cats
#+fs2
import fs2.{ Stream => Process }
import fs2.Stream.{ eval, eval_ }
import fs2.util.{ Monad, Catchable, Suspendable  }
import fs2.interop.cats._
import fs2.interop.cats.reverse.functionKToUf1
#-fs2

import java.sql.Connection
import java.sql.DriverManager
import javax.sql.DataSource

/**
 * Module defining `Transactor`, which abstracts over connection providers and gives natural
 * transformations `ConnectionIO ~> M` and `Process[ConnectionIO, ?] ~> Process[M, ?]` for target
 * monad `M`. By default the resulting computation will be executed on a new connection with
 * `autoCommit` off; will be committed on normal completionand rolled back if an exception escapes;
 * and in all cases the connection will be released properly.
 *
 * This module also provides default implementations backed by `DriverManager` and `DataSouce`.
 */
object transactor {

#+scalaz
  abstract class Transactor[M[_]: Monad: Catchable: Capture] {
#-scalaz
#+fs2
  abstract class Transactor[M[_]: Catchable: Suspendable] {
#-fs2

    /** Action preparing the connection; default is `setAutoCommit(false)`. */
    protected[doobie] def before = setAutoCommit(false)

    /** Action in case of failure; default is `rollback`. */
    protected[doobie] def oops = rollback

    /** Action in case of success; default is `commit`. */
    protected[doobie] def after = commit

    /** Cleanup action run in all cases; default is `close`. */
    protected[doobie] def always = close

    @deprecated("will go away in 0.2.2; use trans", "0.2.1")
    def transact[A](ma: ConnectionIO[A]): M[A] = trans(ma)

    @deprecated("will go away in 0.2.2; use transP", "0.2.1")
    def transact[A](pa: Process[ConnectionIO, A]): Process[M, A] = transP(pa)

    /** Minimal implementation must provide a connection. */
    protected[doobie] def connect: M[Connection]

    /** Unethical syntax for use in the REPL. */
    lazy val yolo = new Yolo(this)

    /** Natural transformation to target monad `M`. */
    object trans extends (ConnectionIO ~> M) {

      private def safe[A](ma: ConnectionIO[A]): ConnectionIO[A] =
        (before *> ma <* after) onException oops ensuring always

      def apply[A](ma: ConnectionIO[A]): M[A] =
        connect.flatMap(c => safe(ma).foldMap(KleisliInterpreter[M].ConnectionInterpreter).run(c))

    }

    /** Natural transformation to an equivalent process over target monad `M`. */
    object transP extends (({ type l[a] = Process[ConnectionIO, a] })#l ~> ({ type l[a] = Process[M, a] })#l) {

      // Convert a ConnectionIO[Unit] to an empty effectful process
      private implicit class VoidProcessOps(ma: ConnectionIO[Unit]) {
        def p: Process[ConnectionIO, Nothing] = eval_(ma)
      }

      private def safe[A](pa: Process[ConnectionIO, A]): Process[ConnectionIO, A] =
#+scalaz
        (before.p ++ pa ++ after.p) onFailure { e => oops.p ++ eval_(delay(throw e)) } onComplete always.p
#-scalaz
#+fs2
        (before.p ++ pa ++ after.p) onError { e => oops.p ++ eval_(delay(throw e)) } onFinalize always
#-fs2

      def apply[A](pa: Process[ConnectionIO, A]): Process[M, A] = {
        import KleisliInterpreter._
        def nat(c: Connection): ConnectionIO ~> M = liftF(KleisliInterpreter[M].ConnectionInterpreter andThen applyKleisli(c))
#+scalaz
        eval(connect).flatMap(c => safe(pa).translate[M](nat(c)))
#-scalaz
#+fs2
        eval(connect).flatMap(c => safe(pa).translate[M](functionKToUf1(nat(c))))
#-fs2
      }

    }

  }

  /** `Transactor` wrapping `java.sql.DriverManager`. */
  object DriverManagerTransactor {

#+scalaz
    def create[M[_]: Monad: Catchable: Capture](driver: String, conn: => Connection): Transactor[M] =
      new Transactor[M] {
        val connect: M[Connection] =
          Capture[M].delay { Class.forName(driver); conn }
      }
#-scalaz
#+fs2
    def create[M[_]: Catchable: Suspendable](driver: String, conn: => Connection): Transactor[M] =
      new Transactor[M] {
        val connect: M[Connection] =
          Suspendable[M].delay { Class.forName(driver); conn }
      }
#-fs2

#+scalaz
    def apply[M[_]: Monad: Catchable: Capture](driver: String, url: String): Transactor[M] =
#-scalaz
#+fs2
    def apply[M[_]: Catchable: Suspendable](driver: String, url: String): Transactor[M] =
#-fs2
      create(driver, DriverManager.getConnection(url))

#+scalaz
    def apply[M[_]: Monad: Catchable: Capture](driver: String, url: String, user: String, pass: String): Transactor[M] =
#-scalaz
#+fs2
    def apply[M[_]: Catchable: Suspendable](driver: String, url: String, user: String, pass: String): Transactor[M] =
#-fs2
      create(driver, DriverManager.getConnection(url, user, pass))

#+scalaz
    def apply[M[_]: Monad: Catchable: Capture](driver: String, url: String, info: java.util.Properties): Transactor[M] =
#-scalaz
#+fs2
    def apply[M[_]: Catchable: Suspendable](driver: String, url: String, info: java.util.Properties): Transactor[M] =
#-fs2
      create(driver, DriverManager.getConnection(url, info))

  }

  /** `Transactor` wrapping an existing `DataSource`. */
#+scalaz
  abstract class DataSourceTransactor[M[_]: Monad: Catchable: Capture, D <: DataSource] private extends Transactor[M] {
#-scalaz
#+fs2
  abstract class DataSourceTransactor[M[_]: Catchable: Suspendable, D <: DataSource] private extends Transactor[M] {
#-fs2
    def configure[A](f: D => M[A]): M[A]
  }

  /** `Transactor` wrapping an existing `DataSource`. */
  object DataSourceTransactor {

    // So we can specify M and infer D.
    class DataSourceTransactorCtor[M[_]] {
#+scalaz
      def apply[D <: DataSource](ds: D)(implicit e0: Monad[M], e1: Catchable[M], e2: Capture[M]): DataSourceTransactor[M ,D] =
        new DataSourceTransactor[M, D] {
          def configure[A](f: D => M[A]): M[A] = f(ds)
          val connect = e2.apply(ds.getConnection)
        }
#-scalaz
#+fs2
      def apply[D <: DataSource](ds: D)(implicit e0: Catchable[M], e1: Suspendable[M]): DataSourceTransactor[M ,D] =
        new DataSourceTransactor[M, D] {
          def configure[A](f: D => M[A]): M[A] = f(ds)
          val connect = e1.delay(ds.getConnection)
        }
#-fs2
    }

    /** Type-curried constructor: construct a new instance via `DataSourceTransactor[M](ds)`. */
    def apply[M[_]]: DataSourceTransactorCtor[M] =
      new DataSourceTransactorCtor[M]

  }

}
