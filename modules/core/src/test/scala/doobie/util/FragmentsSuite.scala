// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.implicits.*

class FragmentsSuite extends munit.FunSuite {
  import Fragments.*
  import cats.effect.unsafe.implicits.global

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.h2.Driver",
    url = "jdbc:h2:mem:queryspec;DB_CLOSE_DELAY=-1",
    user = "sa",
    password = "",
    logHandler = None
  )

  val nelInt = NonEmptyList.of(1, 2, 3)
  val nelIntBool2 = NonEmptyList.of((1, true), (2, false))
  val nelStrDblInt3 = NonEmptyList.of(("abc", 1.2, 3), ("def", 4.5, 6), ("ghi", 7.8, 9))
  val listInt3 = nelInt.toList
  val listIntBool2 = nelIntBool2.toList
  val listStrDblInt3 = nelStrDblInt3.toList
  val nel1 = NonEmptyList.of(1).map(i => sql"$i")
  val nel = NonEmptyList.of(1, 2, 3).map(i => sql"$i")
  val fs = nel.toList
  val someF: Option[Fragment] = Some(sql"${1}")
  val noneF: Option[Fragment] = None
  val ofs = List(Some(sql"${1}"), None, Some(sql"${3}"))
  val sqlKv = nelInt.zipWith(NonEmptyList.of("a", "b", "c"))((v, k) => fr0"${Fragment.const0(k)} = $v").toList

  test("values for one column") {
    assertEquals(values(nelInt).query[Unit].sql, "VALUES (?) , (?) , (?) ")
  }

  test("values for two columns") {
    assertEquals(values(NonEmptyList.of((1, true), (2, false))).query[Unit].sql, "VALUES (?,?) , (?,?) ")
  }

  test("updateSetOpt for three column") {
    assertEquals(
      updateSetOpt(Fragment.const("Foo"), sqlKv).map(_.query[Unit].sql),
      Some("UPDATE Foo SET a = ?, b = ?, c = ?"))
  }

  test("updateSetOpt for empty columns") {
    assertEquals(updateSetOpt(Fragment.const("Foo"), List.empty[Fragment]).map(_.query[Unit].sql), None)
  }

  test("in (1-column, varargs)") {
    assertEquals(
      in(sql"foo.bar", 1, 2).query[Unit].sql,
      "(foo.bar IN (?, ?)) ")
    assertEquals(
      in(sql"foo.bar", 3, 4, 5).query[Unit].sql,
      "(foo.bar IN (?, ?, ?)) ")
  }
  test("in (2-column, varargs)") {
    assertEquals(
      in(sql"foo.bar", 1 -> "one", 2 -> "two").query[Unit].sql,
      "(foo.bar IN ((?,?), (?,?))) ")
    assertEquals(
      in(sql"foo.bar", 3 -> "three", 4 -> "four", 5 -> "five").query[Unit].sql,
      "(foo.bar IN ((?,?), (?,?), (?,?))) ")
  }
  test("in (3-column, varargs)") {
    assertEquals(
      in(sql"foo.bar", (1.2, "A", 3), (4.5, "B", 6)).query[Unit].sql,
      "(foo.bar IN ((?,?,?), (?,?,?))) ")
    assertEquals(
      in(sql"foo.bar", (9.8, "Z", 7), (6.5, "Y", 4), (3.2, "X", 1)).query[Unit].sql,
      "(foo.bar IN ((?,?,?), (?,?,?), (?,?,?))) ")
  }

  test("notIn (1-column, varargs)") {
    assertEquals(
      notIn(sql"foo.bar", 1, 2).query[Unit].sql,
      "(foo.bar NOT IN (?, ?)) ")
    assertEquals(
      notIn(sql"foo.bar", 3, 4, 5).query[Unit].sql,
      "(foo.bar NOT IN (?, ?, ?)) ")
  }
  test("notIn (2-column, varargs)") {
    assertEquals(
      notIn(sql"foo.bar", 1 -> "one", 2 -> "two").query[Unit].sql,
      "(foo.bar NOT IN ((?,?), (?,?))) ")
    assertEquals(
      notIn(sql"foo.bar", 3 -> "three", 4 -> "four", 5 -> "five").query[Unit].sql,
      "(foo.bar NOT IN ((?,?), (?,?), (?,?))) ")
  }
  test("notIn (3-column, varargs)") {
    assertEquals(
      notIn(sql"foo.bar", (1.2, "A", 3), (4.5, "B", 6)).query[Unit].sql,
      "(foo.bar NOT IN ((?,?,?), (?,?,?))) ")
    assertEquals(
      notIn(sql"foo.bar", (9.8, "Z", 7), (6.5, "Y", 4), (3.2, "X", 1)).query[Unit].sql,
      "(foo.bar NOT IN ((?,?,?), (?,?,?), (?,?,?))) ")
  }

  test("in (1-column)") {
    assertEquals(
      in(fr0"foo.bar", nelInt).query[Unit].sql,
      "(foo.bar IN (?, ?, ?)) ")
  }
  test("in (2-columns)") {
    assertEquals(
      in(fr0"foo.bar", nelIntBool2).query[Unit].sql,
      "(foo.bar IN ((?,?), (?,?))) ")
  }
  test("in (3-columns)") {
    assertEquals(
      in(fr0"foo.bar", nelStrDblInt3).query[Unit].sql,
      "(foo.bar IN ((?,?,?), (?,?,?), (?,?,?))) ")
  }

  test("inOpt (1-column, many)") {
    assertEquals(
      inOpt(fr0"foo.bar", listInt3).map(_.query[Unit].sql),
      Some("(foo.bar IN (?, ?, ?)) "))
  }
  test("inOpt (2-columns, many)") {
    assertEquals(
      inOpt(fr0"foo.bar", listIntBool2).map(_.query[Unit].sql),
      Some("(foo.bar IN ((?,?), (?,?))) "))
  }
  test("inOpt (3-columns, many)") {
    assertEquals(
      inOpt(fr0"foo.bar", listStrDblInt3).map(_.query[Unit].sql),
      Some("(foo.bar IN ((?,?,?), (?,?,?), (?,?,?))) "))
  }
  test("inOpt (empty)") {
    assert(inOpt(fr0"foo.bar", List.empty[Int]).isEmpty, "1 column")
    assert(inOpt(fr0"foo.bar", List.empty[(Int, Boolean)]).isEmpty, "2 columns")
    assert(inOpt(fr0"foo.bar", List.empty[(String, Double, Int)]).isEmpty, "3 columns")
  }

  test("notIn (1-column)") {
    assertEquals(
      notIn(fr0"foo.bar", nelInt).query[Unit].sql,
      "(foo.bar NOT IN (?, ?, ?)) ")
  }
  test("notIn (2-columns)") {
    assertEquals(
      notIn(fr0"foo.bar", nelIntBool2).query[Unit].sql,
      "(foo.bar NOT IN ((?,?), (?,?))) ")
  }
  test("notIn (3-columns)") {
    assertEquals(
      notIn(fr0"foo.bar", nelStrDblInt3).query[Unit].sql,
      "(foo.bar NOT IN ((?,?,?), (?,?,?), (?,?,?))) ")
  }

  test("notInOpt (1-column, many)") {
    assertEquals(
      notInOpt(fr0"foo.bar", listInt3).map(_.query[Unit].sql),
      Some("(foo.bar NOT IN (?, ?, ?)) "))
  }
  test("notInOpt (2-columns, many)") {
    assertEquals(
      notInOpt(fr0"foo.bar", listIntBool2).map(_.query[Unit].sql),
      Some("(foo.bar NOT IN ((?,?), (?,?))) "))
  }
  test("notInOpt (3-columns, many)") {
    assertEquals(
      notInOpt(fr0"foo.bar", listStrDblInt3).map(_.query[Unit].sql),
      Some("(foo.bar NOT IN ((?,?,?), (?,?,?), (?,?,?))) "))
  }
  test("notInOpt (empty)") {
    assert(notInOpt(fr0"foo.bar", List.empty[Int]).isEmpty, "1 column")
    assert(notInOpt(fr0"foo.bar", List.empty[(Int, Boolean)]).isEmpty, "2 columns")
    assert(notInOpt(fr0"foo.bar", List.empty[(String, Double, Int)]).isEmpty, "3 columns")
  }

  test("and (vararg 2)") {
    assertEquals(and(fs(0), fs(1)).query[Unit].sql, "((?) AND (?)) ")
  }

  test("and (Reducible 1)") {
    assertEquals(and(nel1).query[Unit].sql, "((?)) ")
  }

  test("and (Reducible many)") {
    assertEquals(and(nel).query[Unit].sql, "((?) AND (?) AND (?)) ")
  }

  test("andOpt (vararg many none)") {
    assertEquals(andOpt(None, None).map(_.query[Unit].sql), None)
  }

  test("andOpt (vararg 1 Some)") {
    assertEquals(andOpt(noneF, someF).map(_.query[Unit].sql), Some("((?)) "))
  }

  test("andOpt (vararg 2 Some)") {
    assertEquals(andOpt(someF, someF).map(_.query[Unit].sql), Some("((?) AND (?)) "))
  }

  test("andOpt (Foldable empty)") {
    assertEquals(andOpt(List.empty[Fragment]).map(_.query[Unit].sql), None)
  }

  test("andOpt (Foldable 1)") {
    assertEquals(andOpt(nel.take(1)).map(_.query[Unit].sql), Some("((?)) "))
  }

  test("andOpt (Foldable many)") {
    assertEquals(andOpt(nel.toList).map(_.query[Unit].sql), Some("((?) AND (?) AND (?)) "))
  }

  test("andOpt (list empty)") {
    assertEquals(andOpt(List.empty[Fragment]).map(_.query[Unit].sql), None)
  }

  test("andFallbackTrue (empty)") {
    assertEquals(andFallbackTrue(List.empty[Fragment]).query[Unit].sql, "TRUE ")
  }

  test("andFallbackTrue (many)") {
    assertEquals(andFallbackTrue(fs).query[Unit].sql, "((?) AND (?) AND (?)) ")
  }

  test("or (vararg 2)") {
    assertEquals(or(fs(0), fs(1)).query[Unit].sql, "((?) OR (?)) ")
  }

  test("or (Reducible 1)") {
    assertEquals(or(nel1).query[Unit].sql, "((?)) ")
  }

  test("or (Reducible many)") {
    assertEquals(or(nel).query[Unit].sql, "((?) OR (?) OR (?)) ")
  }

  test("orOpt (vararg many none)") {
    assertEquals(orOpt(None, None).map(_.query[Unit].sql), None)
  }

  test("orOpt (vararg 1 Some)") {
    assertEquals(orOpt(noneF, someF).map(_.query[Unit].sql), Some("((?)) "))
  }

  test("orOpt (vararg 2 Some)") {
    assertEquals(orOpt(someF, someF).map(_.query[Unit].sql), Some("((?) OR (?)) "))
  }

  test("orOpt (Foldable empty)") {
    assertEquals(orOpt(List.empty[Fragment]).map(_.query[Unit].sql), None)
  }

  test("orOpt (Foldable 1)") {
    assertEquals(orOpt(nel.take(1)).map(_.query[Unit].sql), Some("((?)) "))
  }

  test("orOpt (Foldable many)") {
    assertEquals(orOpt(nel.toList).map(_.query[Unit].sql), Some("((?) OR (?) OR (?)) "))
  }

  test("orOpt (list empty)") {
    assertEquals(orOpt(List.empty[Fragment]).map(_.query[Unit].sql), None)
  }

  test("orFallbackFalse (empty)") {
    assertEquals(orFallbackFalse(List.empty[Fragment]).query[Unit].sql, "FALSE ")
  }

  test("orFallbackFalse (many)") {
    assertEquals(orFallbackFalse(fs).query[Unit].sql, "((?) OR (?) OR (?)) ")
  }

  test("whereAnd (varargs single)") {
    assertEquals(whereAnd(fs(0)).query[Unit].sql, "WHERE (?)")
  }

  test("whereAnd (varargs many)") {
    assertEquals(whereAnd(fs(0), fs(0), fs(0)).query[Unit].sql, "WHERE (?) AND (?) AND (?)")
  }

  test("whereAnd (Reducible 1)") {
    assertEquals(whereAnd(nel1).query[Unit].sql, "WHERE (?)")
  }

  test("whereAnd (Reducible many)") {
    assertEquals(whereAnd(nel).query[Unit].sql, "WHERE (?) AND (?) AND (?)")
  }

  test("whereAndOpt (varargs many Some)") {
    assertEquals(whereAndOpt(someF, someF).query[Unit].sql, "WHERE (?) AND (?)")
  }

  test("whereAndOpt (varargs 1 Some)") {
    assertEquals(whereAndOpt(ofs(0)).query[Unit].sql, "WHERE (?)")
  }

  test("whereAndOpt (varargs all none)") {
    assertEquals(whereAndOpt(None, None).query[Unit].sql, "")
  }

  test("whereAndOpt (Foldable empty)") {
    assertEquals(whereAndOpt(List.empty[Fragment]).query[Unit].sql, "")
  }

  test("whereAndOpt (Foldable many)") {
    assertEquals(whereAndOpt(fs).query[Unit].sql, "WHERE (?) AND (?) AND (?)")
  }

  test("whereOr (varargs single)") {
    assertEquals(whereOr(fs(0)).query[Unit].sql, "WHERE (?)")
  }

  test("whereOr (varargs many)") {
    assertEquals(whereOr(fs(0), fs(0), fs(0)).query[Unit].sql, "WHERE (?) OR (?) OR (?)")
  }

  test("whereOr (Reducible 1)") {
    assertEquals(whereOr(nel1).query[Unit].sql, "WHERE (?)")
  }

  test("whereOr (Reducible many)") {
    assertEquals(whereOr(nel).query[Unit].sql, "WHERE (?) OR (?) OR (?)")
  }

  test("whereOrOpt (varargs many Some)") {
    assertEquals(whereOrOpt(someF, someF).query[Unit].sql, "WHERE (?) OR (?)")
  }

  test("whereOrOpt (varargs 1 Some)") {
    assertEquals(whereOrOpt(ofs(0)).query[Unit].sql, "WHERE (?)")
  }

  test("whereOrOpt (varargs all none)") {
    assertEquals(whereOrOpt(None, None).query[Unit].sql, "")
  }

  test("whereOrOpt (Foldable empty)") {
    assertEquals(whereOrOpt(List.empty[Fragment]).query[Unit].sql, "")
  }

  test("whereOrOpt (Foldable many)") {
    assertEquals(whereOrOpt(fs).query[Unit].sql, "WHERE (?) OR (?) OR (?)")
  }

  test("orderBy (varargs 1)") {
    assertEquals(orderBy(fr0"a").query[Unit].sql, "ORDER BY a")
  }

  test("orderBy (varargs many)") {
    assertEquals(orderBy(fr0"a", fr0"b").query[Unit].sql, "ORDER BY a, b")
  }

  test("orderBy (Reducible 1)") {
    assertEquals(orderBy(NonEmptyList.of(fr0"a")).query[Unit].sql, "ORDER BY a")
  }

  test("orderBy (Reducible many)") {
    assertEquals(orderBy(NonEmptyList.of(fr0"a", fr0"b")).query[Unit].sql, "ORDER BY a, b")
  }

  test("orderByOpt (varargs Some many) ") {
    assertEquals(orderByOpt(Some(fr0"a"), Some(fr0"b")).query[Unit].sql, "ORDER BY a, b")
  }

  test("orderByOpt (varargs all None) ") {
    assertEquals(orderByOpt(None, None).query[Unit].sql, "")
  }

  test("orderByOpt (Foldable empty) ") {
    assertEquals(orderByOpt(List.empty[Fragment]).query[Unit].sql, "")
  }

  test("orderByOpt (Foldable many) ") {
    assertEquals(orderByOpt(List(fr0"a", fr0"b")).query[Unit].sql, "ORDER BY a, b")
  }

  test("Usage test: whereAndOpt") {
    assertEquals(
      whereAndOpt(Some(sql"hi"), orOpt(List.empty[Fragment]), orOpt(List(sql"a", sql"b"))).query[Unit].sql,
      "WHERE (hi) AND (((a) OR (b)) )")
  }

  case class Person(name: String, age: Int)
  case class Contact(person: Person, address: Option[String])

  test("values (1)") {
    val c = Contact(Person("Bob", 42), Some("addr"))
    val f = sql"select" ++ Fragments.values(c)
    assertEquals(f.query[Contact].unique.transact(xa).unsafeRunSync(), c)
  }

  test("values (2)") {
    val c = Contact(Person("Bob", 42), None)
    val f = sql"select" ++ Fragments.values(c)
    assertEquals(f.query[Contact].unique.transact(xa).unsafeRunSync(), c)
  }

}
