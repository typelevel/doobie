package doobie

import scalaz.Show

case class LogElement(s: String)

object LogElement {
  implicit def show: Show[LogElement] =
    Show.shows(_.s)
}

