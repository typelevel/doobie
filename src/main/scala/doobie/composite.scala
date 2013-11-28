package doobie

import scalaz._
import Scalaz._
import world.statement
import world.statement._
import java.sql._
import scala.language.experimental.macros
import shapeless._

// typeclass for values that [potentially] span columns
trait Composite[A] { outer =>

  def set: A => Action[Unit]
  def get: Action[A]

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
      def get = ??? // statement.get
    }

  implicit def inOpt[A: Primitive]: Composite[Option[A]] =
    new Composite[Option[A]] {
      def set = _.fold(statement.setNull)(statement.set) // nice!
      def get = ??? 
    }

  implicit def deriveComposite[A] = 
    macro TypeClass.derive_impl[Composite, A]

  implicit val productComposite: ProductTypeClass[Composite] =
    new ProductTypeClass[Composite] {
  
      def product[H, T <: HList](H: Composite[H], T: Composite[T]): Composite[H :: T] =
        new Composite[H :: T] {
          def set = l => H.set(l.head) >> advance >> T.set(l.tail)
          def get = ??? // (H.get |@| advance |@| T.get)((h, _, t) => h :: t)
        }

      def emptyProduct: Composite[HNil] =
        new Composite[HNil] {
          def set = _ => success(())
          def get = success(HNil)
        }

      def project[F, G](instance: => Composite[G], to: F => G, from: G => F): Composite[F] =
        new Composite[F] {
          def set = f => instance.set(to(f))
          def get = instance.get.map(from)
        }

    }

}

