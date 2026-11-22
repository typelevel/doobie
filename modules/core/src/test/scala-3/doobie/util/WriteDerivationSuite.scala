// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import org.typelevel.doobie.enumerated.Nullability.{NoNulls, Nullable}
import org.typelevel.doobie.util.WriteDerivationSuite.*

class WriteDerivationSuite extends munit.FunSuite {

  test("Automatic derivation of widely nested types (each type used several times per level)") {
    import org.typelevel.doobie.generic.auto.*
    assertEquals(Write[Layer6].length, 9557)
  }

  test("Automatic derivation of diamond-shaped nesting (types shared across levels)") {
    import org.typelevel.doobie.generic.auto.*
    assertEquals(Write[Diamond20].length, 28656)
  }

  test("Derivation of a case class with more fields than the inline limit") {
    assertEquals(Write.derived[Wide100].length, 100)
  }

  test("Automatic derivation flattens nested case classes, options and tuples in field order") {
    import org.typelevel.doobie.generic.auto.*
    val write = Write[Outer]
    assertEquals(write.length, 11)
    assertEquals(write.toList(outer), outerValues)
    assertEquals(write.puts.map(_._2), outerNullability)
  }

  test("Semiauto derivation with explicit nested instances gives the same result as automatic derivation") {
    implicit val innerWrite: Write[Inner] = Write.derived[Inner]
    val write = Write.derived[Outer]
    assertEquals(write.toList(outer), outerValues)
    assertEquals(write.puts.map(_._2), outerNullability)
  }

  test("Option of a derived type writes all columns as null for None") {
    import org.typelevel.doobie.generic.auto.*
    assertEquals(Write[Option[Outer]].toList(None), List.fill(11)(None))
    assertEquals(Write[Option[Outer]].toList(Some(outer)), Write[Outer].toList(outer).map {
      case o: Option[?] => o
      case v            => Some(v)
    })
  }

  test("Derivation prefers a custom Write instance for an Option type") {
    assertEquals(Write.derived[HasTagged].toList(HasTagged(Some(Tagged("x")))), List("x_custom"))

    import org.typelevel.doobie.generic.auto.*
    assertEquals(Write[HasTagged].toList(HasTagged(None)), List("none"))
  }

  test("Write.derived does not derive nested case classes") {
    val errors = compileErrors("Write.derived[NeedsInner]")
    assert(errors.contains("no Write instance found for NeedsInner.inner"), errors)
  }

  test("Write.derived uses automatic derivation for nested case classes when it is enabled") {
    import org.typelevel.doobie.generic.auto.*
    assertEquals(Write.derived[NeedsInner].length, 2)
  }

  test("Derivation fails with a clear error for recursive types") {
    val errors = compileErrors("Write.derived[Recursive]")
    assert(errors.contains("refers to itself"), errors)
  }
}

object WriteDerivationSuite {
  case class Layer0(i: Int, s: String)
  case class Layer1(a: Layer0, b: Layer0, c: Layer0, d: Layer0, s: String)
  case class Layer2(a: Layer1, b: Layer1, c: Layer1, d: Layer1, s: String)
  case class Layer3(a: Layer2, b: Layer2, c: Layer2, d: Layer2, s: String)
  case class Layer4(a: Layer3, b: Layer3, c: Layer3, d: Layer3, s: String)
  case class Layer5(a: Layer4, b: Layer4, c: Layer4, d: Layer4, s: String)
  case class Layer6(a: Layer5, b: Layer5, c: Layer5, d: Layer5, s: String)

  case class Diamond0(i: Int)
  case class Diamond1(a: Diamond0, s: String)
  case class Diamond2(a: Diamond1, b: Diamond0, s: String)
  case class Diamond3(a: Diamond2, b: Diamond1, s: String)
  case class Diamond4(a: Diamond3, b: Diamond2, s: String)
  case class Diamond5(a: Diamond4, b: Diamond3, s: String)
  case class Diamond6(a: Diamond5, b: Diamond4, s: String)
  case class Diamond7(a: Diamond6, b: Diamond5, s: String)
  case class Diamond8(a: Diamond7, b: Diamond6, s: String)
  case class Diamond9(a: Diamond8, b: Diamond7, s: String)
  case class Diamond10(a: Diamond9, b: Diamond8, s: String)
  case class Diamond11(a: Diamond10, b: Diamond9, s: String)
  case class Diamond12(a: Diamond11, b: Diamond10, s: String)
  case class Diamond13(a: Diamond12, b: Diamond11, s: String)
  case class Diamond14(a: Diamond13, b: Diamond12, s: String)
  case class Diamond15(a: Diamond14, b: Diamond13, s: String)
  case class Diamond16(a: Diamond15, b: Diamond14, s: String)
  case class Diamond17(a: Diamond16, b: Diamond15, s: String)
  case class Diamond18(a: Diamond17, b: Diamond16, s: String)
  case class Diamond19(a: Diamond18, b: Diamond17, s: String)
  case class Diamond20(a: Diamond19, b: Diamond18, s: String)

  case class Wide100(
      f1: Int, f2: Int, f3: Int, f4: Int, f5: Int, f6: Int, f7: Int, f8: Int, f9: Int, f10: Int,
      f11: Int, f12: Int, f13: Int, f14: Int, f15: Int, f16: Int, f17: Int, f18: Int, f19: Int, f20: Int,
      f21: Int, f22: Int, f23: Int, f24: Int, f25: Int, f26: Int, f27: Int, f28: Int, f29: Int, f30: Int,
      f31: Int, f32: Int, f33: Int, f34: Int, f35: Int, f36: Int, f37: Int, f38: Int, f39: Int, f40: Int,
      f41: Int, f42: Int, f43: Int, f44: Int, f45: Int, f46: Int, f47: Int, f48: Int, f49: Int, f50: Int,
      f51: Int, f52: Int, f53: Int, f54: Int, f55: Int, f56: Int, f57: Int, f58: Int, f59: Int, f60: Int,
      f61: Int, f62: Int, f63: Int, f64: Int, f65: Int, f66: Int, f67: Int, f68: Int, f69: Int, f70: Int,
      f71: Int, f72: Int, f73: Int, f74: Int, f75: Int, f76: Int, f77: Int, f78: Int, f79: Int, f80: Int,
      f81: Int, f82: Int, f83: Int, f84: Int, f85: Int, f86: Int, f87: Int, f88: Int, f89: Int, f90: Int,
      f91: Int, f92: Int, f93: Int, f94: Int, f95: Int, f96: Int, f97: Int, f98: Int, f99: Int, f100: Int
  )

  case class Inner(a: Int, b: String)
  case class Outer(x: Inner, y: Option[Inner], z: (Int, Inner), w: Option[(Inner, String)], n: Int)

  val outer: Outer = Outer(Inner(1, "a"), Some(Inner(2, "b")), (3, Inner(4, "c")), None, 5)
  val outerValues: List[Any] = List(1, "a", Some(2), Some("b"), 3, 4, "c", None, None, None, 5)
  val outerNullability: List[Any] =
    List(NoNulls, NoNulls, Nullable, Nullable, NoNulls, NoNulls, NoNulls, Nullable, Nullable, Nullable, NoNulls)

  case class Tagged(s: String)
  object Tagged {
    implicit val optionWrite: Write[Option[Tagged]] =
      Write.fromPut[String].contramap(_.fold("none")(_.s.concat("_custom")))
  }
  case class HasTagged(t: Option[Tagged])

  case class NoInstanceInner(i: Int)
  case class NeedsInner(inner: NoInstanceInner, s: String)

  case class Recursive(i: Int, next: Option[Recursive])
}
