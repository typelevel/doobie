// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

opaque type Derived[+I] = I

object Derived:
  def apply[I](i: I): Derived[I] = i

extension [I](d: Derived[I])
  def instance: I = d
