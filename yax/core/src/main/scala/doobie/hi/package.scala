package doobie

import doobie.free.{ connection => C }
import doobie.free.{ driver => D }
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ callablestatement => CS }
import doobie.free.{ resultset => RS }
import doobie.free.{ statement => S }
import doobie.free.{ databasemetadata => DMD }

#+cats
import doobie.util.these.\&/
import doobie.util.these.\&/._
#-cats

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
 *  - An exception to the above rule is that actions consuming or returning Scala `Array` are available
 *    here but use `scalaz.ImmutableArray` or `List`, depending on usage.
 *  - Lifting actions, low-level type mapping actions, and resource management actions do not appear
 *    in this API.
 */
package object hi {

  /** @group Aliases */  type ConnectionIO[A]        = C.ConnectionIO[A]
  /** @group Aliases */  type DriverIO[A]            = D.DriverIO[A]
  /** @group Aliases */  type StatementIO[A]         = S.StatementIO[A]
  /** @group Aliases */  type CallableStatementIO[A] = CS.CallableStatementIO[A]
  /** @group Aliases */  type PreparedStatementIO[A] = PS.PreparedStatementIO[A]
  /** @group Aliases */  type DatabaseMetaDataIO[A]  = DMD.DatabaseMetaDataIO[A]
  /** @group Aliases */  type ResultSetIO[A]         = RS.ResultSetIO[A]

#+cats
  implicit class AlignSyntax[A](as: List[A]) {
    def align[B](bs: List[B]): List[A \&/ B] = {
      def go(as: List[A], bs: List[B], acc: List[A \&/ B]): List[A \&/ B] =
        (as, bs) match {
          case (a :: as, b :: bs) => go(as , bs , Both(a, b) :: acc)
          case (a :: as, Nil    ) => go(as , Nil, This(a)    :: acc)
          case (Nil    , b :: bs) => go(Nil, bs , That(b)    :: acc)
          case (Nil    , Nil    ) => acc.reverse
        }
      go(as, bs, Nil)
    }
  }
#-cats

}
