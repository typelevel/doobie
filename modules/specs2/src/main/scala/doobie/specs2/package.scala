// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

package object specs2 {

  type Checker[M[_]] = analysisspec.Checker[M]
  type IOChecker     = analysisspec.IOChecker
  type AnalysisMatchers = analysismatchers.AnalysisMatchers

  @deprecated(message = "import doobie.specs2._", since = "0.5.0")
  object imports {

    type Checker[M[_]] = analysisspec.Checker[M]
    type IOChecker     = analysisspec.IOChecker
    type AnalysisMatchers = analysismatchers.AnalysisMatchers

    @deprecated("Use IOChecker.", "0.4.2")
    type AnalysisSpec = IOChecker
  }
}
