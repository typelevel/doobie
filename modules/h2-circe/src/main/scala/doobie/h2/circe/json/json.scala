// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.h2.circe

import org.typelevel.doobie.h2.circe.Instances.JsonInstances

package object json {
  object implicits extends JsonInstances
}
