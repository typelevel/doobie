package doobie

import dbc._
import scalaz._
import Scalaz._
import scalaz.effect.IO
import scalaz.effect.kleisliEffect._
import scalaz.syntax.effect.monadCatchIO._

/** Pure functional high-level JDBC layer. */
package object hi {

  def set[A](index: Int, a: A)(implicit A: Comp[A]): PreparedStatement[Unit] =
    preparedstatement.push(s"structured set at index $index: $a", A.set(index, a))

  def get[A](index: Int)(implicit A: Comp[A]): ResultSet[A] =
    resultset.push(s"structured get at index $index", A.get(index))

  // can't do non-consumimh filter or collect; is this a problem?

  /** Consume remaining rows by folding left to right. */
  def foldLeft[A: Comp, B](z: B)(f: (B, A) => B): ResultSet[B] = {
    def foldLeft0(b: B): ResultSet[B] =
      resultset.next >>= {
        case false => b.point[ResultSet]
        case true  => get[A](1) >>= (a => foldLeft0(f(b, a)))
      }
    resultset.push(s"foldLeft($z, $f)", foldLeft0(z))
  }

  /** Consume remaining rows by monoidal fold. */
  def foldMap[A: Comp, B](f: A => B)(implicit B: Monoid[B]): ResultSet[B] =
    resultset.push(s"foldMap($f)", foldLeft[A, B](B.zero)(_ |+| f(_)))

  /** Consume remaining rows by monoidal sum. */
  def fold[A: Comp: Monoid]: ResultSet[A] =
    resultset.push(s"fold", foldMap[A, A](identity))

  /** Discard the next `n` rows. */
  def drop(n: Int): ResultSet[Unit] = 
    resultset.push(s"drop($n)", resultset.relative(n).void)

  /** Consume and return up to `n` remaining rows. */
  def take[A: Comp](n: Int): ResultSet[List[A]] = {
    def take0(n: Int, as: List[A]): ResultSet[List[A]] =
      if (n == 0) as.point[ResultSet] 
      else (resultset.next >>= {
        case true  => get[A](1) >>= { a => take0(n - 1, a :: as) }
        case false => as.point[ResultSet]
      })
    resultset.push(s"take($n)", take0(n, Nil).map(_.reverse))
  }

  /** Consume and return all remaining rows. */
  def list[A: Comp]: ResultSet[List[A]] = 
    resultset.push("list", foldLeft[A, List[A]](Nil)((as, a) => a :: as).map(_.reverse))

  /** Consume all remaining rows by mapping to `A` and passing to effectful action `effect`. */
  def sink[A: Comp](effect: A => IO[Unit]): ResultSet[Unit] = 
    resultset.push(s"sink($effect)", foldMap(effect).flatMap(_.liftIO[ResultSet]))

  /** Consume and return the next value, if any. */
  def headOption[A: Comp]: ResultSet[Option[A]] =
    resultset.push("headOption", 
      resultset.next >>= {
        case true  => get[A](1).map(Some(_))
        case false => None.point[ResultSet]
      })

  /** Consume and return the next value, if any. */
  def unsafeHead[A: Comp]: ResultSet[A] =
    resultset.push("unsafeHead", 
      resultset.next >>= {
        case true  => get[A](1)
        case false => sys.error("head of empty stream")
      })



 import connection.prepareStatement

  implicit class SqlInterpolator(val sc: StringContext) {

    class Source[A: Comp](a: A) {

      def go[B](b: PreparedStatement[B]): Connection[B] =
        prepareStatement(sc.parts.mkString("?"))(set(1, a) >> b)

      def executeQuery[B](b: ResultSet[B]): Connection[B] =
        go(preparedstatement.executeQuery(b))
    
      def execute: Connection[Boolean] =
        go(preparedstatement.execute)

    }

    def sql[A: Prim](a: A) = new Source(a)

    def sql[A: Prim, B: Prim](a: A, b: B) = new Source((a,b))

  }
  
}