package doobie

// Log events
sealed trait Event

object Event {

  case class LoadDriver(name: String) extends Event
  case object OpenConnection extends Event
  case class ConnectionLog(es: Vector[Event]) extends Event
  case object CloseConnection extends Event

  implicit class Memo(msg: String) extends Event {
    override def toString = s"Memo($msg)"
  }

}