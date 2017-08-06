package doobie.scalatest

object imports {

  type Checker[M[_]] = doobie.scalatest.Checker[M]
  type IOChecker = doobie.scalatest.IOChecker

  @deprecated("Use IOChecker.", "0.4.2")
  type QueryChecker = IOChecker

}
