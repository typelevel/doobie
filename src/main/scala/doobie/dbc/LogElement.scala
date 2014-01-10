package doobie
package dbc

import scalaz.Show

case class LogElement(s: String)

object LogElement {
  implicit def show: Show[LogElement] =
    Show.shows(_.s)
}
