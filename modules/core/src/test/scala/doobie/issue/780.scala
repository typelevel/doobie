// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.issue

import doobie._

import scala.annotation.nowarn

@nowarn("msg=.*Foo is never used.*")
class `780` extends munit.FunSuite {

  test("deriving instances should work correctly for Write from class scope") {
    class Foo[A: Write, B: Write] {
      Write[(A, B)]
    }
  }

}
