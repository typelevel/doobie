package doobie
package std

import doobie.JdbcType

object string extends string

trait string {

  def apply(j: JdbcType): Primitive[String] =
    Primitive[String](j, _.setString, _.getString)

  val char         = apply(JdbcType.CHAR)
  val longnvarchar = apply(JdbcType.LONGNVARCHAR)
  val longvarchar  = apply(JdbcType.LONGVARCHAR)
  val nchar        = apply(JdbcType.NCHAR)
  val nvarchar     = apply(JdbcType.NVARCHAR)
  val varchar      = apply(JdbcType.VARCHAR)

  val default = varchar

}