// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.ContravariantSemigroupal
import doobie.enum.Nullability._
import doobie.free.{ FPS, FRS, PreparedStatementIO, ResultSetIO }
import java.sql.{ PreparedStatement, ResultSet }
import shapeless.{ HList, HNil, ::, Generic, Lazy, <:!< }
import shapeless.labelled.{ FieldType }

final class OldWrite[A](
  val puts: List[(Put[_], NullabilityKnown)],
  val toList: A => List[Any],
  val unsafeSet: (PreparedStatement, Int, A) => Unit,
  val unsafeUpdate: (ResultSet, Int, A) => Unit
) {

  lazy val length = puts.length

  def set(n: Int, a: A): PreparedStatementIO[Unit] =
    FPS.raw(unsafeSet(_, n, a))

  def update(n: Int, a: A): ResultSetIO[Unit] =
    FRS.raw(unsafeUpdate(_, n, a))

  def contramap[B](f: B => A): OldWrite[B] =
    new OldWrite(
      puts,
      b => toList(f(b)),
      (ps, n, a) => unsafeSet(ps, n, f(a)),
      (rs, n, a) => unsafeUpdate(rs, n, f(a))
    )

  def product[B](fb: OldWrite[B]): OldWrite[(A, B)] =
    new OldWrite(
      puts ++ fb.puts,
      { case (a, b) => toList(a) ++ fb.toList(b) },
      { case (ps, n, (a, b)) => unsafeSet(ps, n, a); fb.unsafeSet(ps, n + length, b) },
      { case (rs, n, (a, b)) => unsafeUpdate(rs, n, a); fb.unsafeUpdate(rs, n + length, b) }
    )

}

object OldWrite extends LowerPriorityOldWrite {

  def apply[A](implicit A: OldWrite[A]): OldWrite[A] = A

  implicit val ReadContravariantSemigroupal: ContravariantSemigroupal[OldWrite] =
    new ContravariantSemigroupal[OldWrite] {
      def contramap[A, B](fa: OldWrite[A])(f: B => A) = fa.contramap(f)
      def product[A, B](fa: OldWrite[A], fb: OldWrite[B]) = fa.product(fb)
    }

  implicit val unitComposite: OldWrite[Unit] =
    new OldWrite(Nil, _ => Nil, (_, _, _) => (), (_, _, _) => ())

  implicit def fromPut[A](implicit P: Put[A]): OldWrite[A] =
    new OldWrite(
      List((P, NoNulls)),
      a => List(a),
      (ps, n, a) => P.unsafeSetNonNullable(ps, n, a),
      (rs, n, a) => P.unsafeUpdateNonNullable(rs, n, a)
    )

  implicit def fromPutOption[A](implicit P: Put[A]): OldWrite[Option[A]] =
    new OldWrite(
      List((P, Nullable)),
      a => List(a),
      (ps, n, a) => P.unsafeSetNullable(ps, n, a),
      (rs, n, a) => P.unsafeUpdateNullable(rs, n, a)
    )

  implicit def recordOldWrite[K <: Symbol, H, T <: HList](
    implicit H: Lazy[OldWrite[H]],
              T: Lazy[OldWrite[T]]
  ): OldWrite[FieldType[K, H] :: T] = {
    new OldWrite(
      H.value.puts ++ T.value.puts,
      { case h :: t => H.value.toList(h) ++ T.value.toList(t) },
      { case (ps, n, h :: t) => H.value.unsafeSet(ps, n, h); T.value.unsafeSet(ps, n + H.value.length, t) },
      { case (rs, n, h :: t) => H.value.unsafeUpdate(rs, n, h); T.value.unsafeUpdate(rs, n + H.value.length, t) }
    )
  }

}

trait LowerPriorityOldWrite extends EvenLowerPriorityOldWrite {

  implicit def product[H, T <: HList](
    implicit H: Lazy[OldWrite[H]],
              T: Lazy[OldWrite[T]]
  ): OldWrite[H :: T] =
    new OldWrite(
      H.value.puts ++ T.value.puts,
      { case h :: t => H.value.toList(h) ++ T.value.toList(t) },
      { case (ps, n, h :: t) => H.value.unsafeSet(ps, n, h); T.value.unsafeSet(ps, n + H.value.length, t) },
      { case (rs, n, h :: t) => H.value.unsafeUpdate(rs, n, h); T.value.unsafeUpdate(rs, n + H.value.length, t) }
    )

  implicit def emptyProduct: OldWrite[HNil] =
    new OldWrite[HNil](Nil, _ => Nil, (_, _, _) => (), (_, _, _) => ())

  implicit def generic[B, A](implicit gen: Generic.Aux[B, A], A: Lazy[OldWrite[A]]): OldWrite[B] =
    new OldWrite[B](
      A.value.puts,
      b => A.value.toList(gen.to(b)),
      (ps, n, b) => A.value.unsafeSet(ps, n, gen.to(b)),
      (rs, n, b) => A.value.unsafeUpdate(rs, n, gen.to(b))
    )

}

trait EvenLowerPriorityOldWrite {

  implicit val ohnil: OldWrite[Option[HNil]] =
    new OldWrite[Option[HNil]](Nil, _ => Nil, (_, _, _) => (), (_, _, _) => ())

  implicit def ohcons1[H, T <: HList](
    implicit H: Lazy[OldWrite[Option[H]]],
             T: Lazy[OldWrite[Option[T]]],
             N: H <:!< Option[α] forSome { type α }
  ): OldWrite[Option[H :: T]] = {
    void(N)

    def split[A](i: Option[H :: T])(f: (Option[H], Option[T]) => A): A =
      i.fold(f(None, None)) { case h :: t => f(Some(h), Some(t)) }

    new OldWrite(
      H.value.puts ++ T.value.puts,
      split(_) { (h, t) => H.value.toList(h) ++ T.value.toList(t) },
      (ps, n, i) => split(i) { (h, t) => H.value.unsafeSet(ps, n, h); T.value.unsafeSet(ps, n + H.value.length, t) },
      (rs, n, i) => split(i) { (h, t) => H.value.unsafeUpdate(rs, n, h); T.value.unsafeUpdate(rs, n + H.value.length, t) }
    )

  }

  implicit def ohcons2[H, T <: HList](
    implicit H: Lazy[OldWrite[Option[H]]],
             T: Lazy[OldWrite[Option[T]]]
  ): OldWrite[Option[Option[H] :: T]] = {

    def split[A](i: Option[Option[H] :: T])(f: (Option[H], Option[T]) => A): A =
      i.fold(f(None, None)) { case oh :: t => f(oh, Some(t)) }

    new OldWrite(
      H.value.puts ++ T.value.puts,
      split(_) { (h, t) => H.value.toList(h) ++ T.value.toList(t) },
      (ps, n, i) => split(i) { (h, t) => H.value.unsafeSet(ps, n, h); T.value.unsafeSet(ps, n + H.value.length, t) },
      (rs, n, i) => split(i) { (h, t) => H.value.unsafeUpdate(rs, n, h); T.value.unsafeUpdate(rs, n + H.value.length, t) }
    )

  }

  implicit def ogeneric[B, A <: HList](
    implicit G: Generic.Aux[B, A],
             A: Lazy[OldWrite[Option[A]]]
  ): OldWrite[Option[B]] =
    new OldWrite(
      A.value.puts,
      b => A.value.toList(b.map(G.to)),
      (rs, n, a) => A.value.unsafeSet(rs, n, a.map(G.to)),
      (rs, n, a) => A.value.unsafeUpdate(rs, n, a.map(G.to))
    )

}
