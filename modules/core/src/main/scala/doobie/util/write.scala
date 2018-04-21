// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import cats.ContravariantSemigroupal
import doobie.enum.Nullability._
import doobie.free.{ FPS, FRS, PreparedStatementIO, ResultSetIO }
import java.sql.{ PreparedStatement, ResultSet }
import scala.annotation.tailrec
import shapeless.{ HList, HNil, ::, Generic, Lazy, <:!< }
import shapeless.labelled.{ FieldType }

sealed trait Write[A] {

  import scala.collection.immutable.::
  import Write._

  // These are implemented as abstract methods for fastest possible traversal.
  protected def fastUnsafeSet(ps: PreparedStatement, n: Int, a: A): Unit
  protected def fastUnsafeUpdate(rs: ResultSet, n: Int, a: A): Unit

  val puts: List[(Put[_], NullabilityKnown)]
  val length: Int
  protected val height: Int

  final def set(n: Int, a: A): PreparedStatementIO[Unit] =
    FPS.raw(unsafeSet(_, n, a))

  final def update(n: Int, a: A): ResultSetIO[Unit] =
    FRS.raw(unsafeUpdate(_, n, a))

  final def contramap[B](f: B => A): Write[B] =
    Contramap(f, this)

  final def product[B](fb: Write[B]): Write[(A, B)] =
    Contramap2(Predef.identity[(A, B)], this, fb)

  final def toList(a: A): List[Any] = {
    type Item = (α, Write[α]) forSome { type α }

    @tailrec
    def worker(stack: List[Item], acc: List[Any]): List[Any] =
      stack match {
        case Nil => acc
        case h :: rest => h match {
          case (x, NonNullableColumn(_)) =>
            worker(rest, x :: acc)
          case (x, NullableColumn(_)) =>
            worker(rest, x :: acc)
          case (_, Trivial()) =>
            worker(rest, acc)
          case (x, Contramap(f, wy)) =>
            worker((f(x), wy) :: rest, acc)
          case (x, Contramap2(f, wy, wz)) =>
            val (y, z) = f(x)
            // Right to left to save a `.reverse`
            worker((z, wz) :: (y, wy) :: rest, acc)
        }
      }

    worker(List((a, this)), Nil)
  }

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  final def unsafeSet(
    ps: PreparedStatement,
    n: Int,
    a: A,
    stackHeightLimit: Int = defaultStackHeightLimit
  ): Unit = {
    type Item = (α, Write[α]) forSome { type α }

    @tailrec
    def worker(stack: List[Item], k: Int): Unit =
      stack match {
        case Nil => ()
        case h :: rest => h match {
          case (x, wx) if wx.height < stackHeightLimit =>
            wx.fastUnsafeSet(ps, k, x)
            worker(rest, k + wx.length)
          case (x, NonNullableColumn(put)) =>
            put.unsafeSetNonNullable(ps, k, x)
            worker(rest, k + 1)
          case (x, NullableColumn(put)) =>
            put.unsafeSetNullable(ps, k, x)
            worker(rest, k + 1)
          case (_, Trivial()) =>
            worker(rest, k)
          case (x, Contramap(f, wy)) =>
            worker((f(x), wy) :: rest, k)
          case (x, Contramap2(f, wy, wz)) =>
            val (y, z) = f(x)
            // Left to right
            worker((y, wy) :: (z, wz) :: rest, k)
        }
      }

    worker(List((a, this)), n)
  }

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  final def unsafeUpdate(
    rs: ResultSet,
    n: Int,
    a: A,
    stackHeightLimit: Int = defaultStackHeightLimit
  ): Unit = {
    type Item = (α, Write[α]) forSome { type α }

    @tailrec
    def worker(stack: List[Item], k: Int): Unit =
      stack match {
        case Nil => ()
        case h :: rest => h match {
          case (x, wx) if wx.height < stackHeightLimit =>
            wx.fastUnsafeUpdate(rs, k, x)
          case (x, NonNullableColumn(put)) =>
            put.unsafeUpdateNonNullable(rs, k, x)
            worker(rest, k + 1)
          case (x, NullableColumn(put)) =>
            put.unsafeUpdateNullable(rs, k, x)
            worker(rest, k + 1)
          case (_, Trivial()) =>
            worker(rest, k)
          case (x, Contramap(f, wy)) =>
            worker((f(x), wy) :: rest, k)
          case (x, Contramap2(f, wy, wz)) =>
            val (y, z) = f(x)
            // Left to right
            worker((y, wy) :: (z, wz) :: rest, k)
        }
      }

    worker(List((a, this)), n)
  }
}

object Write extends LowerPriorityWrite {

  /**
    * Subtrees with height below this limit will be processed in a stack-based
    * fashion.
    */
  val defaultStackHeightLimit = 64

  private final case class Trivial[A]() extends Write[A] {
    def fastUnsafeSet(ps: PreparedStatement, n: Int, a: A): Unit = ()
    def fastUnsafeUpdate(rs: ResultSet, n: Int, a: A): Unit = ()
    val length = 0
    val height = 1
    val puts = Nil
  }
  private final case class NullableColumn[A](put: Put[A]) extends Write[Option[A]] {
    def fastUnsafeSet(ps: PreparedStatement, n: Int, a: Option[A]): Unit =
      put.unsafeSetNullable(ps, n, a)
    def fastUnsafeUpdate(rs: ResultSet, n: Int, a: Option[A]): Unit =
      put.unsafeUpdateNullable(rs, n, a)
    val length = 1
    val height = 1
    val puts = List((put, Nullable))
  }
  private final case class NonNullableColumn[A](put: Put[A]) extends Write[A] {
    def fastUnsafeSet(ps: PreparedStatement, n: Int, a: A): Unit =
      put.unsafeSetNonNullable(ps, n, a)
    def fastUnsafeUpdate(rs: ResultSet, n: Int, a: A): Unit =
      put.unsafeUpdateNonNullable(rs, n, a)
    val length = 1
    val height = 1
    val puts = List((put, NoNulls))
  }
  private final case class Contramap[A, B](
    f: A => B,
    wb: Write[B]
  ) extends Write[A] {
    def fastUnsafeSet(ps: PreparedStatement, n: Int, a: A): Unit =
      wb.fastUnsafeSet(ps, n, f(a))
    def fastUnsafeUpdate(rs: ResultSet, n: Int, a: A): Unit =
      wb.fastUnsafeUpdate(rs, n, f(a))
    val length = wb.length
    val height = wb.height + 1
    val puts = wb.puts
  }
  private final case class Contramap2[A, B, C](
    f: C => (A, B),
    wa: Write[A],
    wb: Write[B]
  ) extends Write[C] {
    def fastUnsafeSet(ps: PreparedStatement, n: Int, c: C): Unit = {
      val t = f(c)
      wa.fastUnsafeSet(ps, n, t._1)
      wb.fastUnsafeSet(ps, n + wa.length, t._2)
    }
    def fastUnsafeUpdate(rs: ResultSet, n: Int, c: C): Unit = {
      val (a, b) = f(c)
      wa.fastUnsafeUpdate(rs, n, a)
      wb.fastUnsafeUpdate(rs, n + wa.length, b)
    }
    val length = wa.length + wb.length
    val height = Math.max(wa.height, wb.height) + 1
    val puts = wa.puts ++ wb.puts
  }

  def apply[A](implicit A: Write[A]): Write[A] = A

  def contramap2[A, B, C](wa: Write[A], wb: Write[B])(f: C => (A, B)): Write[C] =
    Contramap2(f, wa, wb)
  def trivial[A]: Write[A] = Trivial()
  def nullable[A](implicit put: Put[A]): Write[Option[A]] = NullableColumn(put)
  def nonNullable[A](implicit put: Put[A]): Write[A] = NonNullableColumn(put)

  implicit val WriteContravariantSemigroupal: ContravariantSemigroupal[Write] =
    new ContravariantSemigroupal[Write] {
      def contramap[A, B](fa: Write[A])(f: B => A) = fa.contramap(f)
      def product[A, B](fa: Write[A], fb: Write[B]) = fa.product(fb)
    }

  implicit val unitWrite: Write[Unit] = trivial

  implicit def fromPut[A](implicit P: Put[A]): Write[A] = nonNullable(P)

  implicit def fromPutOption[A](implicit P: Put[A]): Write[Option[A]] = nullable(P)

  implicit def recordWrite[K <: Symbol, H, T <: HList](
    implicit H: Lazy[Write[H]],
              T: Lazy[Write[T]]
  ): Write[FieldType[K, H] :: T] =
    contramap2(H.value, T.value) { case h :: t => (h, t) }
}

trait LowerPriorityWrite extends EvenLowerPriorityWrite {

  import Write._

  implicit def product[H, T <: HList](
    implicit H: Lazy[Write[H]],
              T: Lazy[Write[T]]
  ): Write[H :: T] =
    contramap2(H.value, T.value) { case h :: t => (h, t) }

  implicit def emptyProduct: Write[HNil] = trivial

  implicit def generic[B, A](implicit gen: Generic.Aux[B, A], A: Lazy[Write[A]]): Write[B] =
    A.value.contramap(gen.to)
}

trait EvenLowerPriorityWrite {

  import Write._

  implicit val ohnil: Write[Option[HNil]] = trivial

  implicit def ohcons1[H, T <: HList](
    implicit H: Lazy[Write[Option[H]]],
             T: Lazy[Write[Option[T]]],
             N: H <:!< Option[α] forSome { type α }
  ): Write[Option[H :: T]] = {
    void(N)

    contramap2(H.value, T.value) {
      case Some(h :: t) => (Some(h), Some(t))
      case None => (None, None)
    }
  }

  implicit def ohcons2[H, T <: HList](
    implicit H: Lazy[Write[Option[H]]],
             T: Lazy[Write[Option[T]]]
  ): Write[Option[Option[H] :: T]] =
    contramap2(H.value, T.value) {
      case Some(oh :: t) => (oh, Some(t))
      case None => (None, None)
    }

  implicit def ogeneric[B, A <: HList](
    implicit G: Generic.Aux[B, A],
             A: Lazy[Write[Option[A]]]
  ): Write[Option[B]] =
    A.value.contramap(_.map(G.to))
}
