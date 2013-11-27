package doobie
package param

import scalaz._
import Scalaz._
import world.resultset
import world.resultset._
import java.sql._
import scala.language.experimental.macros
import shapeless._

trait OutParam[A] { outer =>

  def get: Action[A]

  def map[B](f: A => B): OutParam[B] =
    new OutParam[B] {
      def get = outer.get.map(f)
    }

}

object OutParam {

  def apply[A](implicit A: OutParam[A]): OutParam[A] = A

  implicit def out[A](implicit A: Out[A,_]): OutParam[A] =
    new OutParam[A] {
      def get = A.get
    }

  implicit def outOpt[A](implicit A: Out[A,_]): OutParam[Option[A]] =
    new OutParam[Option[A]] {
      def get = ???
    }

  implicit def deriveOutParam[A] = 
    macro TypeClass.derive_impl[OutParam, A]

  implicit val productOutParam: ProductTypeClass[OutParam] =
    new ProductTypeClass[OutParam] {
  
      def product[H, T <: HList](H: OutParam[H], T: OutParam[T]): OutParam[H :: T] =
        new OutParam[H :: T] {
          def get: Action[H :: T] = 
            (H.get |@| T.get)(_ :: _)
        }

      def emptyProduct: OutParam[HNil] =
        new OutParam[HNil] {
          def get: Action[HNil] = 
            success(HNil)
        }

      def project[F, G](instance: => OutParam[G], to: F => G, from: G => F): OutParam[F] =
        new OutParam[F] {
          def get: Action[F] =
            instance.get.map(from)
        }

    }

}
