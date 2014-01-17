package doobie.free

sealed abstract class LogEntry
object LogEntry {
  final case class Root(label: String) extends LogEntry  
  final case class Pending(op: GenOp, start: Nano) extends LogEntry
  final case class Computed(op: GenOp, result: GenResult, start: Nano) extends LogEntry
  final case class Complete(op: GenOp, result: GenResult, duration: Nano) extends LogEntry
}


