package doobie

import scalaz._
import scalaz.syntax.monad._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz.effect.kleisliEffect._

package object dbc {

  type Savepoint = java.sql.Savepoint

  type Connection[+A] = connection.Connection[A]  
  type Statement[+A] = statement.Statement[A]
  type DatabaseMetaData[+A] = databasemetadata.DatabaseMetaData[A]
  type CallableStatement[+A] = callablestatement.CallableStatement[A]
  type PreparedStatement[+A] = preparedstatement.PreparedStatement[A]
  type ResultSet[+A] = resultset.ResultSet[A]

  type LogElement = String // for now
  type Log[L] = util.TreeLogger[L]

  implicit def arrayShow[A](implicit A: Show[A]): Show[Array[A]] =
    Show.shows(as => as.toList.map(A.show).mkString("[", ",", "]'"))

  implicit lazy val sqlConnectionShow = Show.showA[java.sql.Connection]
  implicit lazy val sqlStatementShow = Show.showA[java.sql.Statement]
  implicit lazy val sqlResultSetShow = Show.showA[java.sql.ResultSet]
  implicit lazy val sqlArrayShow = Show.showA[java.sql.Array]
  implicit lazy val sqlDateShow = Show.showA[java.sql.Date]
  implicit lazy val sqlTimeShow = Show.showA[java.sql.Time]
  implicit lazy val sqlTImestampShow = Show.showA[java.sql.Timestamp]
  implicit lazy val sqlBlobShow = Show.showA[java.sql.Blob]
  implicit lazy val sqlClobShow = Show.showA[java.sql.Clob]
  implicit lazy val sqlNClobShow = Show.showA[java.sql.NClob]
  implicit lazy val sqlRefShow = Show.showA[java.sql.Ref]
  implicit lazy val sqlSqlWarningShow = Show.showA[java.sql.SQLWarning]

  implicit lazy val javaBigDecimalShow = Show.showA[java.math.BigDecimal]
  implicit lazy val javaInputStreamShow = Show.showA[java.io.InputStream]
  implicit lazy val javaReaderShow = Show.showA[java.io.Reader]
  implicit lazy val javaObjectShow = Show.showA[java.lang.Object]
  implicit lazy val javaUrlShow = Show.showA[java.net.URL]


}

