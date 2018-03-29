package doobie.postgres.syntax

import cats.Foldable
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.postgres._
import java.io.StringReader

class FragmentOps(f: Fragment) {

  /**
   * Given a fragment of the form `COPY table (col, ...) FROM STDIN` construct a
   * `ConnectionIO` that inserts the values provided in `fa`, returning the number of affected
   * rows.
   */
  def copyIn[F[_]: Foldable, A](fa: F[A])(implicit ev: Text[A]): ConnectionIO[Long] = {
    // Fold with a StringBuilder and unsafeEncode to minimize allocations. Note that inserting no
    // rows is an error so we shortcut on empty input.
    // TODO: stream this rather than constructing the string in memory.
    if (fa.isEmpty) 0L.pure[ConnectionIO] else {
      val data = fa.foldLeft(new StringBuilder) { (b, a) =>
        ev.unsafeEncode(a, b)
        b.append("\n")
      } .toString
      PHC.pgGetCopyAPI(PFCM.copyIn(f.query.sql, new StringReader(data)))
    }
  }

}

trait ToFragmentOps {
  implicit def toFragmentOps(f: Fragment): FragmentOps =
    new FragmentOps(f)
}

object fragment extends ToFragmentOps
