package doobie
package std

import doobie._
import java.net.URL

object url {

  val datalink = Primitive[URL](JdbcType.DATALINK, _.setURL, _.getURL)

  val default = datalink

}