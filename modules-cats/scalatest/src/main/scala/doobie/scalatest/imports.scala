package doobie.scalatest

object imports {

  type Checker[M[_]] = doobie.scalatest.Checker[M]
  type IOLiteChecker = doobie.scalatest.IOLiteChecker

  @deprecated("Use IOLiteChecker.", "0.4.2")
  type QueryChecker = IOLiteChecker

}
