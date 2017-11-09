package doobie
package postgres
package syntax

import cats.Foldable
import cats.syntax.foldable._
import cats.instances.string._
import java.io.{ Reader, StringReader }

class FragmentOps(f: Fragment) {
  def copyIn[F[_]: Foldable, A](fa: F[A])(implicit ev: Csv[A]): ConnectionIO[Long] = {
    val data: Reader = new StringReader(fa.foldMap(s => ev.encode(s, '"', '"') + "\n")) // this is N^2 string append, oops
    PHC.pgGetCopyAPI(PFCM.copyIn(f.query.sql, data))
  }
}

trait ToFragmentOps {
  implicit def toFragmentOps(f: Fragment): FragmentOps =
    new FragmentOps(f)
}

object fragment extends ToFragmentOps
