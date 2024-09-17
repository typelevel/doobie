// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.*
import shapeless.ops.hlist.IsHCons

trait PutPlatform {
  import doobie.util.compat.=:=

  /** @group Instances */
  @deprecated("Use Put.derived instead to derive instances explicitly", "1.0.0-RC6")
  def unaryProductPut[A, L <: HList, H, T <: HList](
      implicit
      G: Generic.Aux[A, L],
      C: IsHCons.Aux[L, H, T],
      H: Lazy[Put[H]],
      E: (H :: HNil) =:= L
  ): MkPut[A] = MkPut.unaryProductPut

}
