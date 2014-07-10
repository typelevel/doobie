package doobie.util

import doobie.util.atom._
import doobie.free._
import doobie.free.resultset.ResultSetIO
import doobie.free.preparedstatement.PreparedStatementIO
import scalaz._, Scalaz._
import shapeless._

/** 
 * Module defining a typeclass for composite database types (those that can map to multiple columns).
 */
object composite {
  
  trait Composite[A] { outer =>

    def set: (Int, A) => PreparedStatementIO[Unit]

    def get: Int => ResultSetIO[A]

    def length: Int // column span

  }

  object Composite {

    def apply[A](implicit A: Composite[A]): Composite[A] = A

    /** @group Typeclass Instances */
    implicit def invariantFunctor: InvariantFunctor[Composite] =
      new InvariantFunctor[Composite] {
        def xmap[A, B](fa:Composite[A], f: A => B, g: B => A): Composite[B] =
          new Composite[B] {
            def set = (i, b) => fa.set(i, g(b))
            def get = i => fa.get(i).map(f)
            def length = fa.length
          }
      }

    /** @group Typeclass Instances */
    implicit def prim[A](implicit A: Atom[A]): Composite[A] =
      new Composite[A] {
        def set = A.set
        def get = A.get
        def length = 1
      }

    /** @group Typeclass Instances */
    implicit val trans: (Atom ~> Composite) =
      new (Atom ~> Composite) {
        def apply[A](fa: Atom[A]): Composite[A] =
          prim(fa)
      }

    /** @group Typeclass Instances */
    implicit def inOpt[A](implicit A: Atom[A]): Composite[Option[A]] =
      new Composite[Option[A]] {
        def set = (i, a) => a.fold(A.setNull(i))(a => A.set(i, a))
        def get = i => A.get(i) >>= (a => resultset.wasNull.map(n => (!n).option(a)))
        def length = 1
      }

    /** @group Typeclass Instances */
    implicit val transOpt: (Atom ~> ({ type l[a] = Composite[Option[a]] })#l) =
      new (Atom ~> ({ type l[a] = Composite[Option[a]] })#l) {
        def apply[A](fa: Atom[A]): Composite[Option[A]] =
          inOpt(fa)
      }

    /** @group Typeclass Instances */
    implicit val productComposite: ProductTypeClass[Composite] =
      new ProductTypeClass[Composite] {
    
        def product[H, T <: HList](H: Composite[H], T: Composite[T]): Composite[H :: T] =
          new Composite[H :: T] {
            def set = (i, l) => H.set(i, l.head) >> T.set(i + H.length, l.tail)
            def get = i => (H.get(i) |@| T.get(i + H.length))(_ :: _)
            def length = H.length + T.length
          }

        def emptyProduct: Composite[HNil] =
          new Composite[HNil] {
            def set = (_, _) => ().point[PreparedStatementIO]
            def get = _ => (HNil : HNil).point[ResultSetIO]
            def length = 0
          }

        def project[F, G](instance: => Composite[G], to: F => G, from: G => F): Composite[F] =
          new Composite[F] {
            def set = (i, f) => instance.set(i, to(f))
            def get = i => instance.get(i).map(from)
            def length = instance.length
          }

      }

    /** @group Typeclass Instances */
    implicit def deriveComposite[T](implicit ev: ProductTypeClass[Composite]): Composite[T] =
      macro GenericMacros.deriveProductInstance[Composite, T]

  }

}
