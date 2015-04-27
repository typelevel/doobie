package doobie.util

import doobie.hi.{ ConnectionIO, PreparedStatementIO }
import doobie.hi.connection.{ prepareStatement, prepareStatementS, prepareUpdateAnalysis, prepareUpdateAnalysis0, updateWithGeneratedKeys }
import doobie.hi.preparedstatement.{ set, executeUpdate, executeUpdateWithUniqueGeneratedKeys, addBatchesAndExecute }
import doobie.util.composite.Composite
import doobie.util.analysis.Analysis

import scalaz.Contravariant
import scalaz.Foldable
import scalaz.std.list._
import scalaz.stream.Process
import scalaz.syntax.monad._
import scalaz.syntax.foldable._

/** Module defining updates parameterized by input type. */
object update {

  /** Mixin trait for queries with diagnostic information. */
  trait UpdateDiagnostics {
    val sql: String
    val stackFrame: Option[StackTraceElement]
    def analysis: ConnectionIO[Analysis]
  }

  trait Update[A] extends UpdateDiagnostics { u =>

    def run(a: A): ConnectionIO[Int]

    def withGeneratedKeys[K: Composite](columns: String*)(a: A): Process[ConnectionIO, K]

    def withUniqueGeneratedKeys[K: Composite](columns: String*)(a: A): ConnectionIO[K]

    def updateMany[F[_]: Foldable](fa: F[A]): ConnectionIO[Int]

    // N.B. this what we want to implement, but updateManyWithGeneratedKeys is what we want to call
    protected def updateManyWithGeneratedKeysA[F[_]: Foldable, K: Composite](columns: String*)(as: F[A]): Process[ConnectionIO, K]

    def updateManyWithGeneratedKeys[K](columns: String*) =
      new Update.UpdateManyWithGeneratedKeysBuilder[A, K] {
        def apply[F[_]](as: F[A])(implicit F: Foldable[F], K: Composite[K]): Process[ConnectionIO, K] =
          updateManyWithGeneratedKeysA[F,K](columns: _*)(as)
      }

    def contramap[C](f: C => A): Update[C] =
      new Update[C] {
        val sql = u.sql
        val stackFrame = u.stackFrame
        def analysis: ConnectionIO[Analysis] = u.analysis
        def run(c: C) = u.run(f(c))
        def updateMany[F[_]: Foldable](fa: F[C]) = u.updateMany(fa.toList.map(f))
        protected def updateManyWithGeneratedKeysA[F[_]: Foldable, K: Composite](columns: String*)(cs: F[C]): Process[ConnectionIO, K] =
          u.updateManyWithGeneratedKeys(columns: _*)(cs.toList map f)
        def withGeneratedKeys[K: Composite](columns: String*)(c: C) =
          u.withGeneratedKeys(columns: _*)(f(c))
        def withUniqueGeneratedKeys[K: Composite](columns: String*)(c: C) =
          u.withUniqueGeneratedKeys(columns: _*)(f(c))
      }

    def toUpdate0(a: A): Update0 =
      new Update0 {
        val sql = u.sql
        val stackFrame = u.stackFrame
        def analysis = u.analysis
        def run = u.run(a)
        def withGeneratedKeys[K: Composite](columns: String*) = 
          u.withGeneratedKeys(columns: _*)(a)
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
      def apply[F[_]](as: F[A])(implicit F: Foldable[F], K: Composite[K]): Process[ConnectionIO, K]
    }

    def apply[A: Composite](sql0: String, stackFrame0: Option[StackTraceElement] = None): Update[A] =
      new Update[A] {
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis: ConnectionIO[Analysis] = prepareUpdateAnalysis[A](sql)
        def run(a: A) = prepareStatement(sql)(set(a) >> executeUpdate)
        def updateMany[F[_]: Foldable](fa: F[A]) =
          prepareStatement(sql)(addBatchesAndExecute(fa))
        protected def updateManyWithGeneratedKeysA[F[_]: Foldable, K: Composite](columns: String*)(as: F[A]) =
          doobie.hi.connection.updateManyWithGeneratedKeys[F,A,K](columns.toList)(sql, ().point[PreparedStatementIO], as)
        def withGeneratedKeys[K: Composite](columns: String*)(a: A) =
          updateWithGeneratedKeys[K](columns.toList)(sql, set(a))
        def withUniqueGeneratedKeys[K: Composite](columns: String*)(a: A) =
          prepareStatementS(sql0, columns.toList)(set(a) >> executeUpdateWithUniqueGeneratedKeys)
      }

    implicit val updateContravariant: Contravariant[Update] =
      new Contravariant[Update] {
        def contramap[A, B](fa: Update[A])(f: B => A) = fa contramap f
      }

  }

  trait Update0 extends UpdateDiagnostics {
    def run: ConnectionIO[Int]
    def withGeneratedKeys[K: Composite](columns: String*): Process[ConnectionIO, K]
    def withUniqueGeneratedKeys[K: Composite](columns: String*): ConnectionIO[K]
  }

  object Update0 {

    def apply(sql0: String, stackFrame0: Option[StackTraceElement]): Update0 =
      new Update0 {
        val sql = sql0
        val stackFrame = stackFrame0
        def analysis: ConnectionIO[Analysis] = prepareUpdateAnalysis0(sql)
        def run = prepareStatement(sql)(executeUpdate)
        def withGeneratedKeys[K: Composite](columns: String*) =
          updateWithGeneratedKeys(columns.toList)(sql, ().point[PreparedStatementIO])
        def withUniqueGeneratedKeys[K: Composite](columns: String*) =
          prepareStatementS(sql0, columns.toList)(executeUpdateWithUniqueGeneratedKeys)
      }

  }

}