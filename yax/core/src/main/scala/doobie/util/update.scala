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
import doobie.util.log.{ LogHandler, LogEvent }
import doobie.util.log.LogEvent._

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

  /** Mixin trait for queries with diagnostic information. */
  trait UpdateDiagnostics {
    val sql: String
    val stackFrame: Option[StackTraceElement]
    def analysis: ConnectionIO[Analysis]
  }

  trait Update[A] extends UpdateDiagnostics { u =>

    val logHandler: Option[LogHandler[A]]
    private val now: PreparedStatementIO[Long] = FPS.delay(System.nanoTime)
    private def fail[T](t: Throwable): PreparedStatementIO[T] = FPS.delay(throw t)

    // Equivalent to HPS.executeUpdate(k) but with logging if logHandler is defined
    private def executeUpdate[T](a: A): PreparedStatementIO[Int] = {
      // N.B. the .attempt syntax isn't working in cats. unclear why
      val c = Predef.implicitly[Catchable[PreparedStatementIO]]
      def diff(a: Long, b: Long) = FiniteDuration((a - b).abs, NANOSECONDS)
      logHandler.fold(HPS.executeUpdate) { h =>
        def log(e: LogEvent[A]) = FPS.delay(h.unsafeRun(e))
        for {
          t0 <- now
          en <- c.attempt(FPS.executeUpdate)
          t1 <- now
          n  <- en match {
                  case -\/(e) => log(ExecFailure(sql, a, diff(t1, t0), e)) *> fail[Int](e)
                  case \/-(a) => a.pure[PreparedStatementIO]
                }
          _  <- log(Success(sql, a, diff(t1, t0), FiniteDuration(0L, NANOSECONDS)))
        } yield n
      }
    }

    def run(a: A): ConnectionIO[Int]

    def withGeneratedKeys[K: Composite](columns: String*)(a: A): Process[ConnectionIO, K] =
      withGeneratedKeysWithChunkSize(columns: _*)(a, DefaultChunkSize)

    def withUniqueGeneratedKeys[K: Composite](columns: String*)(a: A): ConnectionIO[K]

    def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(a: A, chunkSize: Int): Process[ConnectionIO, K]


    def updateMany[F[_]: Foldable](fa: F[A]): ConnectionIO[Int]

    // N.B. this what we want to implement, but updateManyWithGeneratedKeys is what we want to call
    protected def updateManyWithGeneratedKeysA[F[_]: Foldable, K: Composite](columns: String*)(as: F[A], chunkSize: Int): Process[ConnectionIO, K]

    def updateManyWithGeneratedKeys[K](columns: String*): Update.UpdateManyWithGeneratedKeysBuilder[A, K] =
      new Update.UpdateManyWithGeneratedKeysBuilder[A, K] {
        def withChunkSize[F[_]](as: F[A], chunkSize: Int)(implicit F: Foldable[F], K: Composite[K]): Process[ConnectionIO, K] =
          updateManyWithGeneratedKeysA[F,K](columns: _*)(as, chunkSize)
      }

    def contramap[C](f: C => A): Update[C] =
      new Update[C] {
        val sql = u.sql
        val stackFrame = u.stackFrame
        val logHandler = u.logHandler.map(_.contramap(f))
        def analysis: ConnectionIO[Analysis] = u.analysis
        def run(c: C) = u.run(f(c))
        def updateMany[F[_]: Foldable](fa: F[C]) = u.updateMany(fa.toList.map(f))
        protected def updateManyWithGeneratedKeysA[F[_]: Foldable, K: Composite](columns: String*)(cs: F[C], chunkSize: Int): Process[ConnectionIO, K] =
          u.updateManyWithGeneratedKeys(columns: _*).withChunkSize(cs.toList map f, chunkSize)
        def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(c: C, chunkSize: Int) =
          u.withGeneratedKeysWithChunkSize(columns: _*)(f(c), chunkSize)
        def withUniqueGeneratedKeys[K: Composite](columns: String*)(c: C) =
          u.withUniqueGeneratedKeys(columns: _*)(f(c))
      }

    def toUpdate0(a: A): Update0 =
      new Update0 {
        val sql = u.sql
        val stackFrame = u.stackFrame
        def analysis = u.analysis
        def run = u.run(a)
        def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(chunkSize: Int) =
          u.withGeneratedKeysWithChunkSize(columns: _*)(a, chunkSize)
        def withUniqueGeneratedKeys[K: Composite](columns: String*) =
          u.withUniqueGeneratedKeys(columns: _*)(a)
      }

  }

  object Update {

    /**
     * Partial application hack to allow calling updateManyWithGeneratedKeys without passing the
     * F[_] type argument explicitly.
     */
    trait UpdateManyWithGeneratedKeysBuilder[A, K] {
      def apply[F[_]](as: F[A])(implicit F: Foldable[F], K: Composite[K]): Process[ConnectionIO, K] =
        withChunkSize(as, DefaultChunkSize)
      def withChunkSize[F[_]](as: F[A], chunkSize: Int)(implicit F: Foldable[F], K: Composite[K]): Process[ConnectionIO, K]
    }

    def apply[A: Composite](sql0: String, stackFrame0: Option[StackTraceElement] = None, logHandler0: Option[LogHandler[A]] = None): Update[A] =
      new Update[A] {
        val logHandler = logHandler0
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis: ConnectionIO[Analysis] = HC.prepareUpdateAnalysis[A](sql)
        def run(a: A) = HC.prepareStatement(sql)(HPS.set(a) *> HPS.executeUpdate)
        def updateMany[F[_]: Foldable](fa: F[A]) =
          HC.prepareStatement(sql)(HPS.addBatchesAndExecute(fa))

        protected def updateManyWithGeneratedKeysA[F[_]: Foldable, K: Composite](columns: String*)(as: F[A], chunkSize: Int) =
          HC.updateManyWithGeneratedKeys[F,A,K](columns.toList)(sql, ().pure[PreparedStatementIO], as, chunkSize)

        def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(a: A, chunkSize: Int) =
          HC.updateWithGeneratedKeys[K](columns.toList)(sql, HPS.set(a), chunkSize)

        def withUniqueGeneratedKeys[K: Composite](columns: String*)(a: A) =
          HC.prepareStatementS(sql0, columns.toList)(HPS.set(a) *> HPS.executeUpdateWithUniqueGeneratedKeys)

      }

    implicit val updateContravariant: Contravariant[Update] =
      new Contravariant[Update] {
        def contramap[A, B](fa: Update[A])(f: B => A) = fa contramap f
      }

  }

  trait Update0 extends UpdateDiagnostics {
    def run: ConnectionIO[Int]

    def withGeneratedKeys[K: Composite](columns: String*): Process[ConnectionIO, K] =
      withGeneratedKeysWithChunkSize(columns: _*)(DefaultChunkSize)

    def withUniqueGeneratedKeys[K: Composite](columns: String*): ConnectionIO[K]

    def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(chunkSize:Int): Process[ConnectionIO, K]

  }

  object Update0 {

    def apply(sql0: String, stackFrame0: Option[StackTraceElement]): Update0 =
      new Update0 {
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis: ConnectionIO[Analysis] = HC.prepareUpdateAnalysis0(sql)
        def run = HC.prepareStatement(sql)(HPS.executeUpdate)

        def withGeneratedKeysWithChunkSize[K: Composite](columns: String*)(chunkSize: Int) =
          HC.updateWithGeneratedKeys(columns.toList)(sql, ().pure[PreparedStatementIO], chunkSize)

        def withUniqueGeneratedKeys[K: Composite](columns: String*) =
          HC.prepareStatementS(sql0, columns.toList)(HPS.executeUpdateWithUniqueGeneratedKeys)
      }

  }

}
