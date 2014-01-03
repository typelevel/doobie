package doobie
package hi

import scala.language.experimental.macros
import scalaz._
import Scalaz._
import shapeless._
import dbc._

// typeclass for values that can span columns
trait Comp[A] { outer =>

  def set: (Int, A) => PreparedStatement[Unit]
  def get: Int => ResultSet[A]

  def length: Int // column span

  def xmap[B](f: A => B, g: B => A): Comp[B] =
    new Comp[B] {
      def set = (i, b) => outer.set(i, g(b))
      def get = i => outer.get(i).map(f)
      def length = outer.length
    }

}

object Comp {

  def apply[A](implicit A: Comp[A]): Comp[A] = A

  implicit def prim[A](implicit A: Prim[A]): Comp[A] =
    new Comp[A] {
      def set = A.set
      def get = A.get
      def length = 1
    }

  implicit def inOpt[A](implicit A: Prim[A]): Comp[Option[A]] =
    new Comp[Option[A]] {
      def set = (i, a) => a.fold(A.setNull(i))(a => A.set(i, a))
      def get = i => A.get(i) >>= (a => resultset.wasNull.map(n => (!n).option(a)))
      def length = 1
    }

  implicit def deriveComp[A] = 
    macro TypeClass.derive_impl[Comp, A]

  implicit val productComp: ProductTypeClass[Comp] =
    new ProductTypeClass[Comp] {
  
      def product[H, T <: HList](H: Comp[H], T: Comp[T]): Comp[H :: T] =
        new Comp[H :: T] {
          def set = (i, l) => H.set(i, l.head) >> T.set(i + H.length, l.tail)
          def get = i => (H.get(i) |@| T.get(i + H.length))(_ :: _)
          def length = H.length + T.length
        }

      def emptyProduct: Comp[HNil] =
        new Comp[HNil] {
          def set = (_, _) => ().point[PreparedStatement]
          def get = _ => HNil.point[ResultSet]
          def length = 0
        }

      def project[F, G](instance: => Comp[G], to: F => G, from: G => F): Comp[F] =
        new Comp[F] {
          def set = (i, f) => instance.set(i, to(f))
          def get = i => instance.get(i).map(from)
          def length = instance.length
        }

    }

}

