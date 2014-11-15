package doobie.util

          
import doobie.free.connection.{ ConnectionIO, setAutoCommit, commit, rollback, close, delay }
import doobie.hi.connection.ProcessConnectionIOOps
import doobie.syntax.catchable._
import doobie.syntax.process._
import doobie.util.capture._
import doobie.util.query._
import doobie.util.update._
import doobie.util.yolo._

import scalaz.syntax.monad._
import scalaz.stream.Process
import scalaz.stream.Process. { eval, eval_, halt }

import scalaz.{ Monad, Catchable, Kleisli }
import scalaz.stream.Process

import java.sql.Connection

/**
 * Module defining `Transactor`, a type that lifts `ConnectionIO` directly into a target effectful
 * monad.
 */
object transactor {

  abstract class Transactor[M[_]: Monad: Catchable: Capture] {

    private val before = setAutoCommit(false)
    private val oops   = rollback       
    private val after  = commit            
    private val always = close   

    // Convert a ConnectionIO[Unit] to an empty effectful process
    private implicit class VoidProcessOps(ma: ConnectionIO[Unit]) {
      def p: Process[ConnectionIO, Nothing] =
        eval(ma) *> halt
    }

    private def safe[A](ma: ConnectionIO[A]): ConnectionIO[A] =
      (before *> ma <* after) onException oops ensuring always

    private def safe[A](pa: Process[ConnectionIO, A]): Process[ConnectionIO, A] =
      (before.p ++ pa ++ after.p) onFailure { e => oops.p ++ eval_(delay(throw e)) } onComplete always.p

    def transact[A](ma: ConnectionIO[A]): M[A] =
      connect >>= safe(ma).transK[M]

    def transact[A](pa: Process[ConnectionIO, A]): Process[M, A] =
      eval(connect) >>= safe(pa).trans[M]

    // implementors need to give us this
    protected def connect: M[Connection] 

    /** Unethical syntax for use in the REPL. */
    lazy val yolo = new Yolo(this)

  }

  object DriverManagerTransactor {
    import doobie.free.drivermanager.{ delay, getConnection }
    def apply[M[_]: Monad: Catchable: Capture](driver: String, url: String, user: String, pass: String): Transactor[M] =
      new Transactor[M] {
        val connect: M[Connection] =
          (delay(Class.forName(driver)) *> getConnection(url, user, pass)).trans[M]      
      }
    }

}