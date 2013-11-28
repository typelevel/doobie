package doobie

import java.sql._

// typeclass for primitive "column" types
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

}

