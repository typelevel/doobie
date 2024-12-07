// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

trait WritePlatform extends LowestPriorityWrite:

  given tupleBase[H](
      using H: Write[H]
  ): Write[H *: EmptyTuple] =
    Write.Composite[H *: EmptyTuple](
      List(H),
      {
        case h *: EmptyTuple => List(h)
      }
    )

  given tuple[H, T <: Tuple](
      using
      H: Write[H],
      T: Write[T]
  ): Write[H *: T] =
    Write.Composite(
      List(H, T),
      {
        case h *: t => List(h, t)
      }
    )
