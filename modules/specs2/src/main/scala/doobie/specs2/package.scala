package doobie

package object specs2 {

  type Checker[M[_]] = analysisspec.Checker[M]
  type IOChecker     = analysisspec.IOChecker

  @deprecated(message = "import doobie.specs2._", since = "0.5.0")
  object imports {

    type Checker[M[_]] = analysisspec.Checker[M]
    type IOChecker     = analysisspec.IOChecker

    @deprecated("Use IOChecker.", "0.4.2")
    type AnalysisSpec = IOChecker

  }

}
