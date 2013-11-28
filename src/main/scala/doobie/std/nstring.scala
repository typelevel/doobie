package doobie
package std

import doobie._
import java.net.URL

object nstring {

  val nchar        = Primitive[String](JdbcType.NCHAR, _.setNString, _.getNString)
  val nvarchar     = Primitive[String](JdbcType.NVARCHAR, _.setNString, _.getNString)
  val longnvarchar = Primitive[String](JdbcType.LONGNVARCHAR, _.setNString, _.getNString)

  val default = nvarchar

}


