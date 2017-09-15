package example

import doobie.enum.JdbcType
import doobie._
//import doobie.implicits._
import org.postgresql.util._
import cats.data.NonEmptyList
//import cats.effect.IO

/**
  * The normal string mapping doesn't work for enums defined in another schema. Here we have
  *
  *   CREATE TYPE returns_data.return_status AS ENUM (
  *     'INITIAL',
  *     'IN_PROGRESS',
  *     'FINISHED'
  *   );
  *
  */
object OtherSchema {

  // Ok this mapping goes via String when reading and PGObject when writing, and it understands
  // when the type is reported as OTHER (schemaType).
  def wackyPostgresMapping(schemaName: String): Meta[String] =
  Meta.advanced[String](
    NonEmptyList.of(JdbcType.Other, JdbcType.VarChar),
    NonEmptyList.one(schemaName),
    (rs, n) => rs.getString(n),
    (ps, n, a) => {
      val o = new PGobject
      o.setValue(a.toString)
      o.setType(schemaName)
      ps.setObject(n, o)
    },
    (rs, n, a) => {
      val o = new PGobject
      o.setValue(a.toString)
      o.setType(schemaName)
      rs.updateObject(n, o)
    }
  )

//  sealed trait ReturnStatus
//  final case object INITIAL extends ReturnStatus
//  final case object IN_PROGRESS extends ReturnStatus
//  final case object FINISHED extends ReturnStatus
//
//  def toReturnStatus(returnStatusStr: String): ReturnStatus ={
//    returnStatusStr match {
//      case "INITIAL" => INITIAL
//      case "IN_PROGRESS" => IN_PROGRESS
//      case "FINISHED" => FINISHED
//      case _ => FINISHED
//    }
//  }
//
//  object ReturnStatus extends Enumeration {
//    val INITIAL, IN_PROGRESS, FINISHED = Value
//  }
//
//  implicit val meta: Meta[ReturnStatus] =
//    wackyPostgresMapping(""""returns_data"."return_status"""").xmap(toReturnStatus, _.toString)
//
//  def main(args: Array[String]): Unit = {
//
//    // Some setup
//    val xa = Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")
//    val y  = xa.yolo
//    import y._
//
    // Check as column value only
//    val q1 = sql"SELECT 'INITIAL'::returns_data.return_status".query[ReturnStatus]
////    q1.check
//    val ret1 = q1.unique.quick
//
//    // Check as parameter too
//    val q2 = sql"SELECT ${IN_PROGRESS}::returns_data.return_status".query[ReturnStatus]
////    q2.check
//    val ret2 = q2.unique.quick
//    println(s"$ret2")
//  }

}