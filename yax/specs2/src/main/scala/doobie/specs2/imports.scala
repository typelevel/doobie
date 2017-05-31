package doobie.specs2

object imports {

  type Checker[M[_]] = analysisspec.Checker[M]
  type IOLiteChecker = analysisspec.IOLiteChecker
  type TaskChecker   = analysisspec.TaskChecker
#+scalaz
  type IOChecker     = analysisspec.IOChecker
#-scalaz

  @deprecated("Use IOLiteChecker.", "0.4.2")
  type AnalysisSpec = IOLiteChecker

}
