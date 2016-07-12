package doobie

#+cats
import cats._, cats.data._, cats.implicits._
#-cats

/** 
 * Collection of modules for typeclasses and other helpful bits.
 */
package object util {

#+cats
  private[util] implicit class IdOps[A](a: A) {
    def <|[B](f: A => B): A = { f(a); a }
  }

  private[util] implicit class MoreFoldableOps[F[_], A: Eq](fa: F[A])(implicit f: Foldable[F]) {
    def element(a: A): Boolean =
      f.exists(fa)(_ === a)
  }

  private[util] implicit class NelOps[A](as: NonEmptyList[A]) {
    def list: List[A] = as.head :: as.tail
  }  
#-cats

}