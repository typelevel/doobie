package doobie.specs2

object imports {

  type Checker[M[_]] = analysisspec.Checker[M]
  type IOChecker = analysisspec.IOChecker

  @deprecated("Use IOChecker.", "0.4.2")
  type AnalysisSpec = IOChecker

}
