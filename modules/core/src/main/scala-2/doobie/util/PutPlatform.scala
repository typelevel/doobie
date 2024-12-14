// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.*
import shapeless.ops.hlist.IsHCons

trait PutPlatform {
  import doobie.util.compat.=:=

  /** @group Instances */
  def unaryProductPut[A, L <: HList, H, T <: HList](
      implicit
      G: Generic.Aux[A, L],
      C: IsHCons.Aux[L, H, T],
      H: Lazy[Put[H]],
      E: (H :: HNil) =:= L
  ): Put[A] = {
    void(E) // E is a necessary constraint but isn't used directly
    H.value.contramap[A](a => G.to(a).head)
  }

}
