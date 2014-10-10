package doobie.util

import doobie.enum.jdbctype.JdbcType
import doobie.util.scalatype.ScalaType
import doobie.enum.nullability._
import doobie.util.atom._
import doobie.util.invariant._
import doobie.free.resultset.{ ResultSetIO, updateNull }
import doobie.free.preparedstatement.PreparedStatementIO
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }

import scala.annotation.implicitNotFound

import scalaz._, Scalaz._
import shapeless._

import java.sql.ParameterMetaData

/** 
 * Module defining a typeclass for composite database types (those that can map to multiple columns).
 */
object composite {

  @implicitNotFound("Could not find or construct Composite[${A}]; ensure that all members of A have Atom or ScalaType instances.")
  trait Composite[A] { 
    val set: (Int, A) => PS.PreparedStatementIO[Unit]
    val update: (Int, A) => RS.ResultSetIO[Unit]
    val get: Int => RS.ResultSetIO[A]
    val length: Int
    val meta: List[(ScalaType[_], NullabilityKnown)]
  }

  object Composite {

    def apply[A](implicit A: Composite[A]): Composite[A] = A

    implicit def fromAtom[A](implicit A: Atom[A]): Composite[A] =
      new Composite[A] {
        val set = A.set
        val update = A.update
        val get = A.get
        val length = 1
        val meta = List(A.meta)
      }

    /** @group Typeclass Instances */
    implicit val productComposite: ProductTypeClass[Composite] =
      new ProductTypeClass[Composite] {
    
        def product[H, T <: HList](H: Composite[H], T: Composite[T]): Composite[H :: T] =
          new Composite[H :: T] {
            val set = (i: Int, l: H :: T) => H.set(i, l.head) >> T.set(i + H.length, l.tail)
            val update = (i: Int, l: H :: T) => H.update(i, l.head) >> T.update(i + H.length, l.tail)
            val get = (i: Int) => (H.get(i) |@| T.get(i + H.length))(_ :: _)
            val length = H.length + T.length
            val meta = H.meta ++ T.meta
          }

        def emptyProduct: Composite[HNil] =
          new Composite[HNil] {
            val set = (_: Int, _: HNil) => ().point[PS.PreparedStatementIO]
            val update = (_: Int, _: HNil) => ().point[RS.ResultSetIO]
            val get = (_: Int) => (HNil : HNil).point[RS.ResultSetIO]
            val length = 0
            val meta = Nil
          }

        def project[F, G](instance: => Composite[G], to: F => G, from: G => F): Composite[F] =
          new Composite[F] {
            val set = (i: Int, f: F) => instance.set(i, to(f))
            val update = (i: Int, f: F) => instance.update(i, to(f))
            val get = (i: Int) => instance.get(i).map(from)
            val length = instance.length
            val meta = instance.meta
          }

      }

    /** @group Typeclass Instances */
    implicit def deriveComposite[T](implicit ev: ProductTypeClass[Composite]): Composite[T] =
      macro GenericMacros.deriveProductInstance[Composite, T]

  }

}








// object composite_old {
  
//   @implicitNotFound("Could not find or construct a Composite[${A}]; ensure Atom instances are available for all fields in ${A}. For example, if there is an Int field you should import doobie.std.int._")
//   trait Composite[A] { outer =>
//     def set: (Int, A) => PreparedStatementIO[Unit]
//     def update: (Int, A) => ResultSetIO[Unit]
//     def get: Int => ResultSetIO[A]
//     def length: Int // column span
//     def ameta: List[analysis.JdkMeta]
//   }

//   object Composite {

//     def apply[A](implicit A: Composite[A]): Composite[A] = A

//     /** @group Typeclass Instances */
//     implicit def invariantFunctor: InvariantFunctor[Composite] =
//       new InvariantFunctor[Composite] {
//         def xmap[A, B](fa:Composite[A], f: A => B, g: B => A): Composite[B] =
//           new Composite[B] {
//             def set = (i, b) => fa.set(i, g(b))
//             def update = (i, b) => fa.update(i, g(b))
//             def get = i => fa.get(i).map(f)
//             def length = fa.length
//             def ameta = fa.ameta
//           }
//       }

//     implicit def lift[A](implicit A: Unlifted[A]): Composite[Option[A]] =
//       A.lift

//     /** @group Typeclass Instances */
//     implicit val productComposite: ProductTypeClass[Composite] =
//       new ProductTypeClass[Composite] {
    
//         def product[H, T <: HList](H: Composite[H], T: Composite[T]): Composite[H :: T] =
//           new Composite[H :: T] {
//             def set = (i, l) => H.set(i, l.head) >> T.set(i + H.length, l.tail)
//             def update = (i, l) => H.update(i, l.head) >> T.update(i + H.length, l.tail)
//             def get = i => (H.get(i) |@| T.get(i + H.length))(_ :: _)
//             def length = H.length + T.length
//             def ameta = H.ameta ++ T.ameta
//           }

//         def emptyProduct: Composite[HNil] =
//           new Composite[HNil] {
//             def set = (_, _) => ().point[PreparedStatementIO]
//             def update = (_, _) => ().point[ResultSetIO]
//             def get = _ => (HNil : HNil).point[ResultSetIO]
//             def length = 0
//             def ameta = Nil
//           }

//         def project[F, G](instance: => Composite[G], to: F => G, from: G => F): Composite[F] =
//           new Composite[F] {
//             def set = (i, f) => instance.set(i, to(f))
//             def update = (i, f) => instance.update(i, to(f))
//             def get = i => instance.get(i).map(from)
//             def length = instance.length
//             def ameta = instance.ameta
//           }

//       }

//     /** @group Typeclass Instances */
//     implicit def deriveComposite[T](implicit ev: ProductTypeClass[Composite]): Composite[T] =
//       macro GenericMacros.deriveProductInstance[Composite, T]

//   }

// }
