// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

trait ScalaBinaryVersionPlatform {
  def currentVersion: ScalaBinaryVersion = ScalaBinaryVersion.S3
}
