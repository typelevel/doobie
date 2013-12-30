package doobie

import java.sql._

// typeclass for values that map to a single column
@annotation.implicitNotFound("${A} is has no Primitive SQL mapping")
trait Primitive[A] { outer =>

  def set: PreparedStatement => (Int, A) => Unit
  def get: ResultSet => Int => A
  def jdbcType: JdbcType

  // Exponential functor
  def xmap[B](f: A => B, g: B => A): Primitive[B] =
    new Primitive[B] {
      def set = ps => (n, b) => outer.set(ps)(n, g(b))
      def get = ps => n => f(outer.get(ps)(n))
      def jdbcType = outer.jdbcType
    }

  override def toString =
    s"Primitive($jdbcType)"

}

// Smart ctor for now, maybe it should just be a case class
object Primitive {

  def apply[A](
    j: JdbcType, 
    fset: PreparedStatement => (Int, A) => Unit,
    fget: ResultSet => Int => A): Primitive[A] =
  new Primitive[A] {
    def set = fset
    def get = fget
    def jdbcType = j
  }

}