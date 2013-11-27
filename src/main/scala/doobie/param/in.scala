package doobie
package param

import scalaz._
import Scalaz._
import world.statement
import world.statement._
import java.sql._
import scala.language.experimental.macros
import shapeless._

trait InParam[A] { outer => 

  def set: A => Action[Unit]

  def contramap[B](f: B => A): InParam[B] =
    new InParam[B] {
      def set = b => outer.set(f(b))
    }

}

object InParam {

  def apply[A](implicit A: InParam[A]): InParam[A] = A

  implicit def in[A](implicit A: In[A,_]): InParam[A] =
    new InParam[A] {
      def set = A.set
    }

  implicit def inOpt[A](implicit A: In[A,_]): InParam[Option[A]] =
    new InParam[Option[A]] {
      def set = _.fold(A.setNull)(A.set) // nice!
    }

  implicit def deriveInParam[A] = 
    macro TypeClass.derive_impl[InParam, A]

  implicit val productInParam: ProductTypeClass[InParam] =
    new ProductTypeClass[InParam] {
  
      def product[H, T <: HList](H: InParam[H], T: InParam[T]): InParam[H :: T] =
        new InParam[H :: T] {
          def set: H :: T => Action[Unit] = 
            l => H.set(l.head) >> T.set(l.tail)
        }

      def emptyProduct: InParam[HNil] =
        new InParam[HNil] {
          def set: HNil => Action[Unit] = 
            _ => success(())
        }

      def project[F, G](instance: => InParam[G], to: F => G, from: G => F): InParam[F] =
        new InParam[F] {
          def set: F => Action[Unit] =
            f => instance.set(to(f))
        }

    }

}
