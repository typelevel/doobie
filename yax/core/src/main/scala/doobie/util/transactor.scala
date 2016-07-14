package doobie.util

          
import doobie.free.connection.{ ConnectionIO, setAutoCommit, commit, rollback, close, delay }
#+scalaz
import doobie.hi.connection.ProcessConnectionIOOps
#-scalaz
import doobie.syntax.catchable.ToDoobieCatchableOps._
#+scalaz
import doobie.syntax.process._
#-scalaz
import doobie.util.capture._
import doobie.util.query._
import doobie.util.update._
#+cats
import doobie.util.catchable._
#-cats
import doobie.util.yolo._

#+scalaz
import scalaz.syntax.monad._
import scalaz.stream.Process
import scalaz.stream.Process. { eval, eval_, halt }
import scalaz.{ Monad, Catchable, Kleisli, ~> }
import scalaz.stream.Process
#-scalaz
#+cats
import cats.{ Monad, ~> }
import cats.implicits._
#-cats
import java.sql.Connection

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

  abstract class Transactor[M[_]: Monad: Catchable: Capture] {

    /** Action preparing the connection; default is `setAutoCommit(false)`. */
    protected def before = setAutoCommit(false)

    /** Action in case of failure; default is `rollback`. */
    protected def oops = rollback       

    /** Action in case of success; default is `commit`. */
    protected def after = commit            

    /** Cleanup action run in all cases; default is `close`. */
    protected def always = close   

    @deprecated("will go away in 0.2.2; use trans", "0.2.1")
    def transact[A](ma: ConnectionIO[A]): M[A] = trans(ma)

#+scalaz
    @deprecated("will go away in 0.2.2; use transP", "0.2.1")
    def transact[A](pa: Process[ConnectionIO, A]): Process[M, A] = transP(pa)
#-scalaz

    /** Minimal implementation must provide a connection. */
    protected def connect: M[Connection] 

    /** Unethical syntax for use in the REPL. */
    lazy val yolo = new Yolo(this)

    /** Natural transformation to target monad `M`. */
    object trans extends (ConnectionIO ~> M) {
  
      private def safe[A](ma: ConnectionIO[A]): ConnectionIO[A] =
        (before *> ma <* after) onException oops ensuring always

      def apply[A](ma: ConnectionIO[A]): M[A] = 
        connect.flatMap(c => safe(ma).transK[M].run(c))

    }

#+scalaz
    /** Natural transformation to an equivalent process over target monad `M`. */
    object transP extends (({ type l[a] = Process[ConnectionIO, a] })#l ~> ({ type l[a] = Process[M, a] })#l) {

      // Convert a ConnectionIO[Unit] to an empty effectful process
      private implicit class VoidProcessOps(ma: ConnectionIO[Unit]) {
        def p: Process[ConnectionIO, Nothing] = eval(ma) *> halt
      }

      private def safe[A](pa: Process[ConnectionIO, A]): Process[ConnectionIO, A] =
        (before.p ++ pa ++ after.p) onFailure { e => oops.p ++ eval_(delay(throw e)) } onComplete always.p

      def apply[A](pa: Process[ConnectionIO, A]): Process[M, A] = 
        eval(connect) >>= safe(pa).trans[M]

    }
#-scalaz

  }

  /** `Transactor` wrapping `java.sql.DriverManager`. */
  object DriverManagerTransactor {
    import doobie.free.drivermanager.{ delay, getConnection, DriverManagerIO }

    def create[M[_]: Monad: Catchable: Capture](driver: String, conn: DriverManagerIO[Connection]): Transactor[M] =
      new Transactor[M] {
        val connect: M[Connection] =
          (delay(Class.forName(driver)) *> conn).trans[M]
      }

    def apply[M[_]: Monad: Catchable: Capture](driver: String, url: String): Transactor[M] =
      create(driver, getConnection(url))

    def apply[M[_]: Monad: Catchable: Capture](driver: String, url: String, user: String, pass: String): Transactor[M] =
      create(driver, getConnection(url, user, pass))

    def apply[M[_]: Monad: Catchable: Capture](driver: String, url: String, info: java.util.Properties): Transactor[M] =
      create(driver, getConnection(url, info))

  }

  /** `Transactor` wrapping an existing `DataSource`. */
  abstract class DataSourceTransactor[M[_]: Monad: Catchable: Capture, D <: DataSource] private extends Transactor[M] {
    def configure[A](f: D => M[A]): M[A]
  }

  /** `Transactor` wrapping an existing `DataSource`. */
  object DataSourceTransactor {
  
    // So we can specify M and infer D.
    class DataSourceTransactorCtor[M[_]] {
      def apply[D <: DataSource](ds: D)(implicit e0: Monad[M], e1: Catchable[M], e2: Capture[M]): DataSourceTransactor[M ,D] =
        new DataSourceTransactor[M, D] {
          def configure[A](f: D => M[A]): M[A] = f(ds)
          val connect = e2.apply(ds.getConnection)
        }
    }

    /** Type-curried constructor: construct a new instance via `DataSourceTransactor[M](ds)`. */
    def apply[M[_]]: DataSourceTransactorCtor[M] =
      new DataSourceTransactorCtor[M]

  }

}
