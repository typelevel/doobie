package doobie.hi

trait Aliases extends doobie.free.Aliases {
  val HC  = doobie.hi.connection
  val HS  = doobie.hi.statement
  val HPS = doobie.hi.preparedstatement
  val HRS = doobie.hi.resultset
}
object Aliases extends Aliases
