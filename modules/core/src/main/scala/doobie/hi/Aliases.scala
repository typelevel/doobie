package doobie.hi

trait Modules {
  lazy val HC  = doobie.hi.connection
  lazy val HS  = doobie.hi.statement
  lazy val HPS = doobie.hi.preparedstatement
  lazy val HRS = doobie.hi.resultset
}
