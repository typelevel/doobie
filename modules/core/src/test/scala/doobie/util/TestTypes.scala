// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

object TestTypes {
  case object CaseObj

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
  
  case class Big23CaseClass(
      i1: Int,
      i2: Int,
      i3: Int,
      i4: Int,
      i5: Int,
      i6: Int,
      i7: Int,
      i8: Int,
      i9: Int,
      i10: Int,
      i11: Int,
      i12: Int,
      i13: Int,
      i14: Int,
      i15: Int,
      i16: Int,
      i17: Int,
      i18: Int,
      i19: Int,
      i20: Int,
      i21: Int,
      i22: Int,
      i23: Int
  )

}
