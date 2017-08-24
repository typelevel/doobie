// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

package object scalatest {

  @deprecated(message = "import doobie.scalatest._", since = "0.5.0")
  object imports {

    type Checker[M[_]] = scalatest.Checker[M]
    type IOChecker     = scalatest.IOChecker

    @deprecated("Use IOChecker.", "0.4.2")
    type QueryChecker = IOChecker

  }

}
