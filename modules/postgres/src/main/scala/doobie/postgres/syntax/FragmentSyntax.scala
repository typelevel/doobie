package doobie
package postgres
package syntax

import cats.{ Foldable }
import cats.syntax.foldable._
import java.io.StringReader

class FragmentOps(f: Fragment) {
  def copyIn[F[_]: Foldable, A](fa: F[A])(implicit ev: Csv[A]): ConnectionIO[Long] = {
    // Fold with a StringBuilder and unsafeEncode to minimize allocations
    val data = fa.foldLeft(new StringBuilder)((b, a) => ev.unsafeEncode(a, '"', '"')(b).append("\n")).toString
    PHC.pgGetCopyAPI(PFCM.copyIn(f.query.sql, new StringReader(data)))
  }
}

trait ToFragmentOps {
  implicit def toFragmentOps(f: Fragment): FragmentOps =
    new FragmentOps(f)
}

object fragment extends ToFragmentOps
