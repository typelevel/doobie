package doobie.hi

trait Aliases extends Modules

trait Modules {
  val HC  = doobie.hi.connection
  val HS  = doobie.hi.statement
  val HPS = doobie.hi.preparedstatement
  val HRS = doobie.hi.resultset
}
