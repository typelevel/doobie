// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.data.NonEmptyList
import cats.syntax.all._
import doobie._, doobie.implicits._
import cats.effect.IO


class FragmentsSuite extends munit.FunSuite {
  import Fragments._
  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    "org.h2.Driver",
    "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    "sa", ""
  )

  val nel  = List(1,2,3).toNel.getOrElse(sys.error("unpossible"))
  val fs   = List(1,2,3).map(n => fr"$n")
  val ofs  = List(1,2,3).map(n => Some(fr"$n").filter(_ => n % 2 =!= 0))

  test("values for one column") {
    assertEquals(values(nel).query[Unit].sql, "VALUES (?) , (?) , (?) ")
  }

  test("values for two columns") {
    assertEquals(values(NonEmptyList.of((1, true), (2, false))).query[Unit].sql, "VALUES (?,?) , (?,?) ")
  }

  test("in for one column") {
    assertEquals(in(fr"foo", nel).query[Unit].sql, "(foo IN (? , ? , ? ) ) ")
  }

  test("in for two columns") {
    assertEquals(inPairs(fr"foo", NonEmptyList.of((1, true), (2, false))).query[Unit].sql, "(foo IN ((?,?), (?,?)) ) ")
  }

  test("notIn") {
    assertEquals(notIn(fr"foo", nel).query[Unit].sql, "(foo NOT IN (? , ? , ? ) ) ")
  }

  test("and (many)") {
    assertEquals(and(fs).query[Unit].sql, "(? AND ? AND ? ) ")
  }

  test("and (two)") {
    assertEquals(and(fs(0), fs(1)).query[Unit].sql, "(? AND ? ) ")
  }

  test("and (empty)") {
    assertEquals(and(List[Fragment]()).query[Unit].sql, "? ")
  }

  test("andOpt (many)") {
    assertEquals(andOpt(ofs).query[Unit].sql, "(? AND ? ) ")
  }

  test("andOpt (one)") {
    assertEquals(andOpt(ofs(0)).query[Unit].sql, "(? ) ")
  }

  test("andOpt (none)") {
    assertEquals(andOpt(None, None).query[Unit].sql, "? ")
  }

  test("or (many)") {
    assertEquals(or(fs).query[Unit].sql, "(? OR ? OR ? ) ")
  }

  test("or (two)") {
    assertEquals(or(fs(0), fs(1)).query[Unit].sql, "(? OR ? ) ")
  }

  test("or (empty)") {
    assertEquals(or(List[Fragment]()).query[Unit].sql, "? ")
  }

  test("orOpt (many)") {
    assertEquals(orOpt(ofs).query[Unit].sql, "(? OR ? ) ")
  }

  test("orOpt (one)") {
    assertEquals(orOpt(ofs(0)).query[Unit].sql, "(? ) ")
  }

  test("orOpt (none)") {
    assertEquals(orOpt(None, None).query[Unit].sql, "? ")
  }

  test("whereAnd (many)") {
    assertEquals(whereAnd(fs).query[Unit].sql, "WHERE (? AND ? AND ? ) ")
  }

  test("whereAnd (single)") {
    assertEquals(whereAnd(fs(0)).query[Unit].sql, "WHERE (? ) ")
  }

  test("whereAnd (empty)") {
    assertEquals(whereAnd(List[Fragment]()).query[Unit].sql, "")
  }

  test("whereAndOpt (many)") {
    assertEquals(whereAndOpt(ofs: _*).query[Unit].sql, "WHERE (? AND ? ) ")
  }

  test("whereAndOpt (one)") {
    assertEquals(whereAndOpt(ofs(0)).query[Unit].sql, "WHERE (? ) ")
  }

  test("whereAndOpt (none)") {
    assertEquals(whereAndOpt(None, None).query[Unit].sql, "")
  }

  test("whereOr (many)") {
    assertEquals(whereOr(fs).query[Unit].sql, "WHERE (? OR ? OR ? ) ")
  }

  test("whereOr (single)") {
    assertEquals(whereOr(fs(0)).query[Unit].sql, "WHERE (? ) ")
  }

  test("whereOr (empty)") {
    assertEquals(whereOr(List[Fragment]()).query[Unit].sql, "")
  }

  test("whereOrOpt (many)") {
    assertEquals(whereOrOpt(ofs: _*).query[Unit].sql, "WHERE (? OR ? ) ")
  }

  test("whereOrOpt (one)") {
    assertEquals(whereOrOpt(ofs(0)).query[Unit].sql, "WHERE (? ) ")
  }

  test("whereOrOpt (none)") {
    assertEquals(whereOrOpt(None, None).query[Unit].sql, "")
  }

  case class Person(name: String, age: Int)
  case class Contact(person: Person, address: Option[String])

  test("values (1)") {
    val c = Contact(Person("Bob", 42), Some("addr"))
    val f = fr"select" ++ Fragments.values(c)
    assertEquals(f.query[Contact].unique.transact(xa).unsafeRunSync(), c)
  }

  test("values (2)") {
    val c = Contact(Person("Bob", 42), None)
    val f = fr"select" ++ Fragments.values(c)
    assertEquals(f.query[Contact].unique.transact(xa).unsafeRunSync(), c)
  }

}
