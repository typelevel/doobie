package doobie
package postgres
package syntax

import cats.{ Foldable }
import cats.syntax.foldable._
import java.io.StringReader

class FragmentOps(f: Fragment) {

  /**
   * Given a fragment of the form `COPY table (col, ...) FROM STDIN WITH (FORMAT csv)` construct a
   * `ConnectionIO` that inserts the values provided in `fa`, returning the number of affected
   * rows.
   */
  def copyIn[F[_]: Foldable, A](fa: F[A])(implicit ev: Csv[A]): ConnectionIO[Long] = {
    // Fold with a StringBuilder and unsafeEncode to minimize allocations
    // TODO: stream this rather than constructing the string in memory.
    val data = fa.foldLeft(new StringBuilder)((b, a) => ev.unsafeEncode(a, '"', '"')(b).append("\n")).toString
    PHC.pgGetCopyAPI(PFCM.copyIn(f.query.sql, new StringReader(data)))
  }

}

trait ToFragmentOps {
  implicit def toFragmentOps(f: Fragment): FragmentOps =
    new FragmentOps(f)
}

object fragment extends ToFragmentOps
