package doobie

import scalaz._
import Scalaz._
import world.statement
import world.statement._
import java.sql._
import scala.language.experimental.macros
import shapeless._

trait Param[A] {
  def set: A => Action[Unit]
}

object Param {

  def apply[A](implicit A: Param[A]): Param[A] = A

  implicit def inout[A](implicit A: InOut[A,_]): Param[A] =
    new Param[A] {
      def set = A.in.set
    }

  implicit def inOpt[A](implicit A: InOut[A,_]): Param[Option[A]] =
    new Param[Option[A]] {
      def set = _.fold(A.in.setNull)(A.in.set) // nice!
    }

  implicit def deriveParam[A] = 
    macro TypeClass.derive_impl[Param, A]

  implicit val productParam: ProductTypeClass[Param] =
    new ProductTypeClass[Param] {
  
      def product[H, T <: HList](H: Param[H], T: Param[T]): Param[H :: T] =
        new Param[H :: T] {
          def set: H :: T => Action[Unit] = 
            l => H.set(l.head) >> T.set(l.tail)
        }

      def emptyProduct: Param[HNil] =
        new Param[HNil] {
          def set: HNil => Action[Unit] = 
            _ => success(())
        }

      def project[F, G](instance: => Param[G], to: F => G, from: G => F): Param[F] =
        new Param[F] {
          def set: F => Action[Unit] =
            f => instance.set(to(f))
        }

    }

}
