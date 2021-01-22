// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

object UnaryTest {
  case class CaseClass(x: String)
  case class CaseClassAnyval(x: String) extends AnyVal
  class ClassAnyVal(val x: String) extends AnyVal

  case class CaseClassNo(x: String, y: Int)
  class ClassNo(val x: String, val y: Int)
  case class CaseClassEmptyNo()
  case object CaseObjectNo
  object ObjectNo

  /* this is unary, but magnolia doesnt seem to work with this shape */
  class ClassUnaryNo(val x: String)
}

class UnaryTest extends munit.FunSuite {
  test("should be defined for case class with one member") {
    Unary[UnaryTest.CaseClass]
  }
  test("should be defined for class <: AnyVal with one member") {
    Unary[UnaryTest.ClassAnyVal]
  }
  test("should be defined for case class <: AnyVal with one member") {
    Unary[UnaryTest.CaseClassAnyval]
  }

  test("implementation restriction: is not defined for plain classes with one member") {
    compileErrors("Unary[UnaryTest.ClassUnaryNo]")
  }

  test("should not be defined for primitives") {
    compileErrors("Unary[Int]")
    compileErrors("Unary[Unit]")
  }

  test("should not be defined for case class with two members") {
    compileErrors("Unary[UnaryTest.CaseClassNo]")
  }

  test("should not be defined for class with two members") {
    compileErrors("Unary[UnaryTest.ClassNo]")
  }

  test("should not be defined for case object") {
    compileErrors("Unary[UnaryTest.CaseObject.type]")
  }

  test("should not be defined for object") {
    compileErrors("Unary[UnaryTest.Object.type]")
  }
}
