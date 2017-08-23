package doobie

import cats.data.Ior

/**
 * High-level database API. The constructors here are defined
 * in terms of those in `doobie.free.connection` but differ in the following ways:
 *
 *  - Enumerated values represented by `Int` values in JDBC are mapped to one of the proper types
 *    defined in `doobie.enum`.
 *  - Nullable values are represented in terms of `Option`.
 *  - Java collection types are translated to immutable Scala equivalents.
 *  - Actions that compute liftime-managed resources do not return the resource directly, but rather
 *    take a continuation in the resource's monad.
 *  - Actions that compute values of impure types (`CLOB`, `InputStream`, etc.) do not appear in this API.
 *    They are available in the low-level API but must be used with considerable caution.
 *  - Lifting actions, low-level type mapping actions, and resource management actions do not appear
 *    in this API.
 */
package object hi {

  implicit class AlignSyntax[A](as: List[A]) {
    def align[B](bs: List[B]): List[A Ior B] = {
      def go(as: List[A], bs: List[B], acc: List[A Ior B]): List[A Ior B] =
        (as, bs) match {
          case (a :: as, b :: bs) => go(as , bs , Ior.Both(a, b) :: acc)
          case (a :: as, Nil    ) => go(as , Nil, Ior.Left(a)    :: acc)
          case (Nil    , b :: bs) => go(Nil, bs , Ior.Right(b)    :: acc)
          case (Nil    , Nil    ) => acc.reverse
        }
      go(as, bs, Nil)
    }
  }

}
