// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import shapeless.{ HList, HNil, ::, Generic, Lazy}
import shapeless.labelled.FieldType

trait WritePlatform extends LowerPriorityWrite {

  implicit def recordWrite[K <: Symbol, H, T <: HList](implicit H: Lazy[Write[H]], T: Lazy[Write[T]]): Write[FieldType[K, H] :: T] = {
    new Write(
      puts = H.value.puts ++ T.value.puts,
      toList = { case h :: t => H.value.toList(h) ++ T.value.toList(t)},
      unsafeSet = { case (ps, n, h :: t) =>
        H.value.unsafeSet(ps, n, h)
        T.value.unsafeSet(ps, n + H.value.length, t)
      },
      unsafeUpdate = { case (rs, n, h :: t) =>
        H.value.unsafeUpdate(rs, n, h)
        T.value.unsafeUpdate(rs, n + H.value.length, t)
      },
      unsafeSetOption = { case (ps, n, oht) =>
        H.value.unsafeSetOption(ps, n, oht.map(_.head))
        T.value.unsafeSetOption(ps, n + H.value.length, oht.map(_.tail))
      },
      unsafeUpdateOption = { case (rs, n, oht) =>
        H.value.unsafeUpdateOption(rs, n, oht.map(_.head))
        T.value.unsafeUpdateOption(rs, n + H.value.length, oht.map(_.tail))
      },
    )
  }

}

trait LowerPriorityWrite {
  implicit def product[H, T <: HList](implicit H: Lazy[Write[H]], T: Lazy[Write[T]]): Write[H :: T] =
    new Write(
      puts = H.value.puts ++ T.value.puts,
      toList = { case h :: t => H.value.toList(h) ++ T.value.toList(t) },
      unsafeSet = { case (ps, n, h :: t) =>
        H.value.unsafeSet(ps, n, h)
        T.value.unsafeSet(ps, n + H.value.length, t)
      },
      unsafeUpdate = { case (rs, n, h :: t) =>
        H.value.unsafeUpdate(rs, n, h)
        T.value.unsafeUpdate(rs, n + H.value.length, t)
      },
      unsafeSetOption = { case (ps, n, oht) =>
        H.value.unsafeSetOption(ps, n, oht.map(_.head))
        T.value.unsafeSetOption(ps, n + H.value.length, oht.map(_.tail))
      },
      unsafeUpdateOption = { case (rs, n, oht) =>
        H.value.unsafeUpdateOption(rs, n, oht.map(_.head))
        T.value.unsafeUpdateOption(rs, n + H.value.length, oht.map(_.tail))
      },
    )

  implicit val emptyProduct: Write[HNil] =
    new Write[HNil](
      puts = Nil,
      toList = _ => Nil,
      unsafeSet = (_, _, _) => (),
      unsafeUpdate = (_, _, _) => (),
      unsafeSetOption = (_, _, _) => (),
      unsafeUpdateOption = (_, _, _) => (),
    )

  implicit def generic[B, A](implicit gen: Generic.Aux[B, A], A: Lazy[Write[A]]): Write[B] = {
    A.value.contramap(gen.to)
  }
}

