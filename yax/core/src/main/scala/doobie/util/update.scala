package doobie.util

import scala.Predef.longWrapper
import scala.concurrent.duration.{ FiniteDuration, NANOSECONDS }

import doobie.free.connection.ConnectionIO
import doobie.free.{ preparedstatement => FPS }
import doobie.free.preparedstatement.PreparedStatementIO
import doobie.hi.{ connection => HC }
import doobie.hi.{ preparedstatement => HPS }
import doobie.util.analysis.Analysis
import doobie.util.composite.Composite
import doobie.util.log._

#+scalaz
import scalaz.{ Contravariant, Foldable, Catchable, -\/, \/- }
import scalaz.std.list._
import scalaz.stream.Process
import scalaz.syntax.catchable._
import scalaz.syntax.monad._
import scalaz.syntax.foldable._
#-scalaz
#+cats
import cats.Foldable
import cats.functor.Contravariant
import cats.implicits._
import scala.{ Left => -\/, Right => \/- }
#-cats
#+fs2
import fs2.util.Catchable
import fs2.{ Stream => Process }
#-fs2

/** Module defining updates parameterized by input type. */
object update {

  val DefaultChunkSize = query.DefaultChunkSize

  /**
   * Partial application hack to allow calling updateManyWithGeneratedKeys without passing the
   * F[_] type argument explicitly.
   */
  trait UpdateManyWithGeneratedKeysPartiallyApplied[A, K] {
    def apply[F[_]](as: F[A])(implicit F: Foldable[F], K: Composite[K]): Process[ConnectionIO, K] =
      withChunkSize(as, DefaultChunkSize)
    def withChunkSize[F[_]](as: F[A], chunkSize: Int)(implicit F: Foldable[F], K: Composite[K]): Process[ConnectionIO, K]
  }

  /**
   * An update parameterized by some input type `A`. This is the type constructed by the `sql`
   * interpolator.
   */
  trait Update[A] { u =>

    // Contravariant coyoneda trick for A
    protected type I
    protected val ai: A => I
    protected implicit val ic: Composite[I]

    val logHandler: LogHandler
    private val now: PreparedStatementIO[Long] = FPS.delay(System.nanoTime)
    private def fail[T](t: Throwable): PreparedStatementIO[T] = FPS.delay(throw t)

    // Equivalent to HPS.executeUpdate(k) but with logging if logHandler is defined
    private def executeUpdate[T](a: A): PreparedStatementIO[Int] = {
      // N.B. the .attempt syntax isn't working in cats. unclear why
      val args = ic.toList(ai(a))
      val c = Predef.implicitly[Catchable[PreparedStatementIO]]
      def diff(a: Long, b: Long) = FiniteDuration((a - b).abs, NANOSECONDS)
      def log(e: LogEvent) = FPS.delay(logHandler.unsafeRun(e))
      for {
        t0 <- now
        en <- c.attempt(FPS.executeUpdate)
        t1 <- now
        n  <- en match {
                case -\/(e) => log(ExecFailure(sql, args, diff(t1, t0), e)) *> fail[Int](e)
                case \/-(a) => a.pure[PreparedStatementIO]
              }
        _  <- log(Success(sql, args, diff(t1, t0), FiniteDuration(0L, NANOSECONDS)))
      } yield n
    }

    /**
     * The SQL string.
     * @group Diagnostics
     */
    val sql: String

    /**
     * An optional `[[StackTraceElement]]` indicating the source location where this `[[Query]]` was
     * constructed. This is used only for diagnostic purposes.
     * @group Diagnostics
     */
    val stackFrame: Option[StackTraceElement]

    /**
     * Program to construct an analysis of this query's SQL statement and asserted parameter types.
     * @group Diagnostics
     */
    def analysis: ConnectionIO[Analysis] =
      HC.prepareUpdateAnalysis[I](sql)

    def run(a: A): ConnectionIO[Int] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeUpdate)

    def withGeneratedKeys[K: Composite](columns: String*)(a: A): Process[ConnectionIO, K] =
      withGeneratedKeysWithChunkSize[K](columns: _*)(a, DefaultChunkSize)

    def withUniqueGeneratedKeys[K: Composite](columns: String*)(a: A): ConnectionIO[K] =
      HC.prepareStatementS(sql, columns.toList)(HPS.set(ai(a)) *> HPS.executeUpdateWithUniqueGeneratedKeys)

    def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(a: A, chunkSize: Int): Process[ConnectionIO, K] =
      HC.updateWithGeneratedKeys[K](columns.toList)(sql, HPS.set(ai(a)), chunkSize)

    def updateMany[F[_]: Foldable](fa: F[A]): ConnectionIO[Int] =
      HC.prepareStatement(sql)(HPS.addBatchesAndExecute(fa.toList.map(ai)))

    // N.B. this what we want to implement, but updateManyWithGeneratedKeys is what we want to call
    protected def updateManyWithGeneratedKeysA[F[_]: Foldable, K: Composite](columns: String*)(as: F[A], chunkSize: Int): Process[ConnectionIO, K] =
      HC.updateManyWithGeneratedKeys[List,I,K](columns.toList)(sql, ().pure[PreparedStatementIO], as.toList.map(ai), chunkSize)

    def updateManyWithGeneratedKeys[K](columns: String*): UpdateManyWithGeneratedKeysPartiallyApplied[A, K] =
      new UpdateManyWithGeneratedKeysPartiallyApplied[A, K] {
        def withChunkSize[F[_]](as: F[A], chunkSize: Int)(implicit F: Foldable[F], K: Composite[K]): Process[ConnectionIO, K] =
          updateManyWithGeneratedKeysA[F,K](columns: _*)(as, chunkSize)
      }

    /** @group Transformations */
    def contramap[C](f: C => A): Update[C] =
      new Update[C] {
        type I  = u.I
        val ai  = u.ai compose f
        val ic  = u.ic
        val sql = u.sql
        val stackFrame = u.stackFrame
        val logHandler = u.logHandler
      }

    /**
     * Apply an argument, yielding a residual `[[Update0]]`.
     * @group Transformations
     */
    def toUpdate0(a: A): Update0 =
      new Update0 {
        val sql = u.sql
        val stackFrame = u.stackFrame
        def analysis = u.analysis
        def run = u.run(a)
        def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(chunkSize: Int) =
          u.withGeneratedKeysWithChunkSize[K](columns: _*)(a, chunkSize)
        def withUniqueGeneratedKeys[K: Composite](columns: String*) =
          u.withUniqueGeneratedKeys(columns: _*)(a)
      }

  }

  object Update {

    def apply[A](sql0: String, stackFrame0: Option[StackTraceElement] = None, logHandler0: LogHandler = LogHandler.nop)(
      implicit C: Composite[A]
    ): Update[A] =
      new Update[A] {
        type I  = A
        val ai  = (a: A) => a
        val ic  = C
        val sql = sql0
        val logHandler = logHandler0
        val stackFrame = stackFrame0
      }

    implicit val updateContravariant: Contravariant[Update] =
      new Contravariant[Update] {
        def contramap[A, B](fa: Update[A])(f: B => A) = fa contramap f
      }

  }

  trait Update0 {

    val sql: String

    val stackFrame: Option[StackTraceElement]

    def analysis: ConnectionIO[Analysis]

    def run: ConnectionIO[Int]

    def withGeneratedKeys[K: Composite](columns: String*): Process[ConnectionIO, K] =
      withGeneratedKeysWithChunkSize(columns: _*)(DefaultChunkSize)

    def withUniqueGeneratedKeys[K: Composite](columns: String*): ConnectionIO[K]

    def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(chunkSize:Int): Process[ConnectionIO, K]

  }

  object Update0 {

    def apply(sql0: String, stackFrame0: Option[StackTraceElement]): Update0 =
      Update[Unit](sql0, stackFrame0).toUpdate0(())

  }

}
