// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

object TestTypes {
  case object CaseObj
  case class ZeroFieldCaseClass()

  case class TrivialCaseClass(i: Int)
  case class SimpleCaseClass(i: Option[Int], s: String, os: Option[String])
  case class ComplexCaseClass(sc: SimpleCaseClass, osc: Option[SimpleCaseClass], i: Option[Int], s: String)
  case class WrappedSimpleCaseClass(sc: SimpleCaseClass)

  case class HasCustomReadWrite0(c: CustomReadWrite, s: String)
  case class HasCustomReadWrite1(s: String, c: CustomReadWrite)
  case class HasOptCustomReadWrite0(c: Option[CustomReadWrite], s: String)
  case class HasOptCustomReadWrite1(s: String, c: Option[CustomReadWrite])

  case class CustomReadWrite(s: String)

  object CustomReadWrite {
    implicit val write: Write[CustomReadWrite] = Write.fromPut[String].contramap(a => a.s.concat("_W"))
    implicit val read: Read[CustomReadWrite] = Read.fromGet[String].map(str => CustomReadWrite(str.concat("_R")))
  }

  case class HasCustomGetPut0(c: CustomGetPut, s: String)
  case class HasCustomGetPut1(s: String, c: CustomGetPut)
  case class HasOptCustomGetPut0(c: Option[CustomGetPut], s: String)
  case class HasOptCustomGetPut1(s: String, c: Option[CustomGetPut])

  case class CustomGetPut(s: String)

  object CustomGetPut {
    implicit val put: Put[CustomGetPut] = Put[String].contramap(a => a.s.concat("_P"))
    implicit val get: Get[CustomGetPut] = Get[String].tmap(a => CustomGetPut(a.concat("_G")))
  }

  case class Big30CaseClass(
      f1: Int,
      f2: Int,
      f3: Int,
      f4: Int,
      f5: Int,
      f6: Int,
      f7: Option[Int],
      f8: Int,
      f9: Int,
      f10: Int,
      f11: Int,
      f12: Int,
      f13: Int,
      f14: Int,
      f15: String,
      f16: Int,
      f17: Int,
      f18: Option[String],
      f19: Int,
      f20: Int,
      f21: Int,
      f22: Int,
      f23: Int,
      f24: Int,
      f25: Int,
      f26: Int,
      f27: Int,
      f28: Int,
      f29: Int,
      f30: Int
  )

}
