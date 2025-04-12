// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

sealed trait ScalaBinaryVersion

object ScalaBinaryVersion extends ScalaBinaryVersionPlatform {
  case object S2_12 extends ScalaBinaryVersion
  case object S2_13 extends ScalaBinaryVersion
  case object S3 extends ScalaBinaryVersion
}
