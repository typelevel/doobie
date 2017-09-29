package doobie.specs2

import cats.{Id, ~>}
import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.effect.{ Async }
import cats.syntax.list._
import doobie.imports._
import doobie.util.analysis._
import doobie.util.pretty._
import doobie.util.pos.Pos
import scala.reflect.runtime.universe.TypeTag

package util {

  /**
    * Provide support for unsafe transaction execution.
    */
  trait UnsafeTransactions[M[_]] {

    // Effect type, required instances, unsafe run
    implicit val M: Async[M]
    def unsafeRunSync[A](ma: M[A]): A

    def transactor: Transactor[M]

    implicit lazy val analysisSupport: UnsafeTransactionSupport =
      UnsafeTransactionSupport.instance(
        transactor,
        λ[M ~> Id](unsafeRunSync(_))
      )
  }

  /** Common data for all query-like types. */
  final case class AnalysisArgs(
    typeName: String,
    pos: Option[Pos],
    sql: String,
    analysis: ConnectionIO[Analysis]
  ) {
    val cleanedSql = Block(
      sql.lines
        .map(_.trim)
        .filterNot(_.isEmpty)
        .toList
    )

    private val location =
      pos
        .map(f => s"${f.file}:${f.line}")
        .getOrElse("(source location unknown)")

    val header: String = s"$typeName defined at $location"
  }

  /** Information from [[Analysis]], prepared for output. */
  final case class AnalysisReport(
    header: String,
    sql: Block,
    items: List[AnalysisReport.Item]
  ) {
    val succeeded: Boolean = items.forall(_.error.isEmpty)
  }

  object AnalysisReport {
    final case class Item(description: String, error: Option[Block])
  }

  /** Allows unsafe SQL execution. */
  trait UnsafeTransactionSupport {
    def unsafeTransactSync[A](ma: ConnectionIO[A]): Either[Throwable, A]
  }

  object UnsafeTransactionSupport {
    def apply(implicit ev: UnsafeTransactionSupport): UnsafeTransactionSupport = ev

    def instance[F[_]](
      transactor: Transactor[F],
      unsafeRunSync: FunctionK[F, Id]
    )(implicit F: Async[F]): UnsafeTransactionSupport =
      new UnsafeTransactionSupport {
        def unsafeTransactSync[A](ma: ConnectionIO[A]) =
          unsafeRunSync(F.attempt(transactor.trans.apply(ma)))
      }
  }

  /** Typeclass for query-like objects. */
  trait Analyzable[T] {
    def unpack(t: T): AnalysisArgs
  }

  object Analyzable {
    def apply[T](implicit ev: Analyzable[T]): Analyzable[T] = ev

    def unpack[T](t: T)(implicit T: Analyzable[T]): AnalysisArgs =
      T.unpack(t)

    def instance[T](
      impl: T => AnalysisArgs
    ): Analyzable[T] =
      new Analyzable[T] {
        def unpack(t: T) = impl(t)
      }

    implicit def analyzableQuery[A, B](
      implicit A: TypeTag[A], B: TypeTag[B]
    ): Analyzable[Query[A, B]] = instance { q =>
      AnalysisArgs(
        s"Query[${typeName(A)}, ${typeName(B)}]",
        q.pos, q.sql, q.analysis
      )
    }

    implicit def analyzableQuery0[A](
      implicit A: TypeTag[A]
    ): Analyzable[Query0[A]] = instance { q =>
      AnalysisArgs(
        s"Query0[${typeName(A)}]",
        q.pos, q.sql, q.analysis
      )
    }

    implicit def analyzableUpdate[A](
      implicit A: TypeTag[A]
    ): Analyzable[Update[A]] = instance { q =>
      AnalysisArgs(
        s"Update[${typeName(A)}]",
        q.pos, q.sql, q.analysis
      )
    }

    implicit val analyzableUpdate0: Analyzable[Update0] =
      instance { q =>
        AnalysisArgs(
          s"Update0",
          q.pos, q.sql, q.analysis
        )
      }

  }
}

package object util {

  def analyze(args: AnalysisArgs)(
    implicit support: UnsafeTransactionSupport
  ): AnalysisReport =
    AnalysisReport (
      args.header,
      args.cleanedSql,
      support.unsafeTransactSync(args.analysis) match {
        case Left(e) =>
          List(AnalysisReport.Item(
            "SQL Compiles and TypeChecks",
            Some(Block.fromLines(e.getMessage))
          ))
        case Right(a) =>
          AnalysisReport.Item("SQL Compiles and TypeChecks", None) ::
            (a.paramDescriptions ++ a.columnDescriptions)
            .map { case (s, es) =>
              AnalysisReport.Item(s, es.toNel.map(alignmentErrorsToBlock))
            }
      }
    )

  private val packagePrefix = "\\b[a-z]+\\.".r

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  def typeName[A](typeName: TypeTag[A]): String =
    packagePrefix.replaceAllIn(typeName.tpe.toString, "")


  private def alignmentErrorsToBlock(
    es: NonEmptyList[AlignmentError]
  ): Block =
    Block(es.toList.flatMap(_.msg.lines))
}
