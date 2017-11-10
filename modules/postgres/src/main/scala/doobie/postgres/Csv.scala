package doobie.postgres

import doobie._

import cats.Foldable
import cats.syntax.foldable._
import cats.instances.string._

import shapeless.{ HList, HNil, ::, <:!<, Generic }

trait Csv[A] { outer =>
  def unsafeEncode(a: A, quote: Char, esc: Char): StringBuilder => StringBuilder
  final def encode(a: A, quote: Char, esc: Char): String =
    unsafeEncode(a, quote, esc)(new StringBuilder).toString
  final def contramap[B](f: B => A): Csv[B] =
    Csv.instance((b, q, e) => outer.unsafeEncode(f(b), q, e))
}
object Csv extends CsvInstances {
  def apply[A](implicit ev: Csv[A]): ev.type = ev
  def instance[A](f: (A, Char, Char) => StringBuilder => StringBuilder): Csv[A] =
    new Csv[A] {
      def unsafeEncode(a: A, quote: Char, esc: Char) = sb => f(a, quote, esc)(sb)
    }
}

trait CsvInstances extends CsvInstances0 { this: Csv.type =>

  // String requires escaping
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Equals"))
  implicit val stringInstance: Csv[String] =
    instance { (s, q, e) => sb =>
      sb.append(q)
      s.foreach { c =>
        if (c == q) sb.append(e)
        sb.append(c)
      }
      sb.append(q)
    }

  // Primitive Numerics
  implicit val intInstance:    Csv[Int]    = instance { (n, _, _) => _ append n }
  implicit val shortInstance:  Csv[Short]  = instance { (n, _, _) => _ append n }
  implicit val longInstance:   Csv[Long]   = instance { (n, _, _) => _ append n }
  implicit val floatInstance:  Csv[Float]  = instance { (n, _, _) => _ append n }
  implicit val doubleInstance: Csv[Double] = instance { (n, _, _) => _ append n }

  // Big Numerics
  implicit val bigDecimalInstance: Csv[BigDecimal] = instance { (n, _, _) => _ append n.toString }
  implicit val bigIntInstance:     Csv[BigInt]     = instance { (n, _, _) => _ append n.toString }

  // Boolean
  implicit val booleanInstance: Csv[Boolean] =
    instance {
      case (true,  _, _) => _ append "TRUE"
      case (false, _, _) => _ append "FALSE"
    }

  // Date, Time, etc.

  // Byte arrays in E'\xx01A3DD...' format.
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  implicit val byteArrayInstance: Csv[Array[Byte]] =
    instance { (bs, q, e) => sb =>
      sb.append("""E'\\x""")
      sb.append(BigInt(1, bs).toString(16))
      sb.append('\'')
    }

  // Any non-option Csv can be lifted to Option
  implicit def option[A](
    implicit csv: Csv[A],
             nope: A <:!< Option[X] forSome { type X }
  ): Csv[Option[A]] =
    instance {
      case (Some(a), q, e) => csv.unsafeEncode(a, q, e)
      case (None, q, e)    => identity
    }

  // HNil isn't a valid Csv but a single-element HList is
  implicit def single[A](
    implicit csv: Csv[A]
  ): Csv[A :: HNil] =
    csv.contramap(_.head)

  // HLists of more that one element
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  implicit def multiple[H, T <: HList](
    implicit h: Csv[H],
             t: Csv[T]
  ): Csv[H :: T] =
    instance { (l, q, e) => sb =>
      h.unsafeEncode(l.head, q, e)(sb)
      sb.append(',')
      t.unsafeEncode(l.tail, q, e)(sb)
    }

  // Generic
  implicit def generic[A, B](
    implicit gen: Generic.Aux[A, B],
             csv: Csv[B]
  ): Csv[A] =
    csv.contramap(gen.to)

}

trait CsvInstances0 extends CsvInstances1 { this: Csv.type =>

  // Iterable and views thereof, as [nested] ARRAY
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var"))
  implicit def iterableInstance[F[_], A](
    implicit ev: Csv[A],
             f:  F[A] => Iterable[A]
  ): Csv[F[A]] =
    instance { (fa, q, e) => sb =>
      var first = true
      sb.append("ARRAY[")
      f(fa).foreach { a =>
        if (first) first = false
        else sb.append(',')
        ev.unsafeEncode(a, q, e)(sb)
      }
      sb.append(']')
      sb
    }

}

trait CsvInstances1 { this: Csv.type =>

  // Foldable, not as fast
  implicit def foldableInstance[F[_]: Foldable, A](
    implicit ev: Csv[A]
  ): Csv[F[A]] =
    iterableInstance[List, A].contramap(_.toList)

}
