// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

import cats.implicits._
import doobie.implicits._
import org.specs2.mutable.Specification


class putspec extends Specification {

  "put syntax" should {

    "convert to a fragment" in {
      (fr"SELECT" ++ 1.fr).query[Unit].sql must_== "SELECT ? "
    }

    "convert to a fragment0" in {
      (fr"SELECT" ++ 1.fr0).query[Unit].sql must_== "SELECT ?"
    }

    "convert an option to a fragment" in {
      (fr"SELECT" ++ Some(1).fr).query[Unit].sql must_== "SELECT ? "
    }

    "convert an option to a fragment0" in {
      (fr"SELECT" ++ Some(1).fr0).query[Unit].sql must_== "SELECT ?"
    }

    "work in a map" in {
      List(1, 2, 3).foldMap(_.fr).query[Unit].sql must_== "? ? ? "
    }

    "work in a map with fr0" in {
      List(1, 2, 3).foldMap(_.fr0).query[Unit].sql must_== "???"
    }

  }

}
