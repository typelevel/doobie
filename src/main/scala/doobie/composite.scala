package doobie

import scalaz._
import Scalaz._
import java.sql._
import scala.language.experimental.macros
import shapeless._

import world._
import world.statement._
import world.resultset._

// typeclass for values that [potentially] span columns
trait Composite[A] { outer =>

  def set: A => statement.Action[Unit]
  def get: resultset.Action[A]

  // Exponential functor
  def xmap[B](f: A => B, g: B => A): Composite[B] =
    new Composite[B] {
      def set = b => outer.set(g(b))
      def get = outer.get.map(f)
    }

}

object Composite {

  def apply[A](implicit A: Composite[A]): Composite[A] = A

  implicit def prim[A: Primitive]: Composite[A] =
    new Composite[A] {
      def set = statement.set
      def get = resultset.read
    }

  implicit def inOpt[A: Primitive]: Composite[Option[A]] =
    new Composite[Option[A]] {
      def set = _.fold(statement.setNull)(statement.set)
      def get = read >>= (a => wasNull.map(!_).map(_ option a))
    }

  implicit def deriveComposite[A] = 
    macro TypeClass.derive_impl[Composite, A]

  implicit val productComposite: ProductTypeClass[Composite] =
    new ProductTypeClass[Composite] {
  
      def product[H, T <: HList](H: Composite[H], T: Composite[T]): Composite[H :: T] =
        new Composite[H :: T] {
          def set = l => H.set(l.head) >> statement.advance >> T.set(l.tail)
          def get = (H.get |@| resultset.advance |@| T.get)((h, _, t) => h :: t)
        }

      def emptyProduct: Composite[HNil] =
        new Composite[HNil] {
          def set = _ => statement.success(())
          def get = resultset.success(HNil)
        }

      def project[F, G](instance: => Composite[G], to: F => G, from: G => F): Composite[F] =
        new Composite[F] {
          def set = f => instance.set(to(f))
          def get = instance.get.map(from)
        }

    }

}

