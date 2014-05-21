package doobie.util

import java.sql.SQLException
import scalaz._
import scalaz.syntax.Ops
import Scalaz._
import scalaz.effect.MonadCatchIO
import scalaz.syntax.effect.monadCatchIO._

object MonadCatchSql extends MonadCatchSqlFunctions

/** Some SQLException-specific stuff for MonadCatchIO */
trait MonadCatchSqlFunctions {

  def catchSql[M[_]: MonadCatchIO, A](sqlState: String)(ma: M[A])(recover: SQLException => M[A]): M[A] =
    ma except { 
      case e: SQLException => recover(e) 
      case t => throw t
    }

  def catchSqlState[M[_]: MonadCatchIO, A](sqlState: String)(ma: M[A])(recover: M[A]): M[A] =
    catchSql(sqlState)(ma)(e => if (e.getSQLState === sqlState) recover else throw e)

  def catchSqlStateLeft[M[_]: MonadCatchIO, A, B](sqlState: String)(ma: M[A])(recover: M[B]): M[B \/ A] =
    catchSqlState[M, B \/ A](sqlState)(ma.map(_.right))(recover.map(_.left))

  def catchSqlStateOption[M[_]: MonadCatchIO, A](sqlState: String)(ma: M[A]): M[Option[A]] =
    catchSqlState(sqlState)(ma.map(_.some))(none.point[M])

}

trait MonadCatchSqlOps[M[_], A] extends Ops[M[A]] {

  implicit def M: MonadCatchIO[M]

  def catchSql(sqlState: String)(recover: SQLException => M[A]): M[A] =
    MonadCatchSql.catchSql(sqlState)(self)(recover)

  def catchSqlState(sqlState: String)(recover: M[A]): M[A] =
    MonadCatchSql.catchSqlState(sqlState)(self)(recover)

  def catchSqlStateLeft[B](sqlState: String)(recover: M[B]): M[B \/ A] =
    MonadCatchSql.catchSqlStateLeft(sqlState)(self)(recover)

  def catchSqlStateOption(sqlState: String): M[Option[A]] =
    MonadCatchSql.catchSqlStateOption(sqlState)(self)

}

trait ToMonadCatchSqlOps {

  implicit def toMonadCatchSqlOps[M[_], A](ma: M[A])(implicit M0: MonadCatchIO[M]): MonadCatchSqlOps[M, A] =
    new MonadCatchSqlOps[M, A] {
      val self = ma
      val M = M0
    }

}
