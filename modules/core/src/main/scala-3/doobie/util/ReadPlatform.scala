// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

trait ReadPlatform extends LowestPriorityRead:

  given tupleBase[H](
      using H: Read[H]
  ): Read[H *: EmptyTuple] =
    H.map(h => h *: EmptyTuple)

  given tuple[H, T <: Tuple](
      using
      H: Read[H],
      T: Read[T]
  ): Read[H *: T] =
    Read.Composite(H, T, (h, t) => h *: t)
