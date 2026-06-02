// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import shapeless.*
import shapeless.ops.hlist.IsHCons

trait GetPlatform {
  import org.typelevel.doobie.util.compat.=:=

  /** @group Instances */
  def unaryProductGet[A, L <: HList, H, T <: HList](
      implicit
      G: Generic.Aux[A, L],
      C: IsHCons.Aux[L, H, T],
      H: Lazy[Get[H]],
      E: (H :: HNil) =:= L
  ): Get[A] = {
    void(C) // C drives inference but is not used directly
    H.value.tmap[A](h => G.from(h :: HNil))
  }

}
