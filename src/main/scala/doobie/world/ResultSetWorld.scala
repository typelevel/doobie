package doobie
package world

import doobie.JdbcType
import java.sql.ResultSet
import scalaz._
import Scalaz._

object ResultSetWorld extends IndexedWorld {

  type R = ResultSet

  def W = implicitly[Monoid[W]]

  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeRun(rs: ResultSet) = 
      run(State(rs, Vector(), 1), a)
  }

  sealed class Out[A, J] private (f: ResultSet => Index => A)(implicit J: JdbcType[J]) { 

    def get: Action[A] = 
      next(f(_)(_))

    def map[B](g: A => B): Out[B, J] =
      new Out[B, J](rs => n => g(f(rs)(n)))

  }

  object Out {

    def apply[A, J: JdbcType](f: ResultSet => Index => A): Out[A, J] = new Out[A, J](f)

    implicit def functor[J]: Functor[({type λ[α] = Out[α, J]})#λ] =
      new Functor[({type λ[α] = Out[α, J]})#λ] {
        def map[A, B](r: Out[A, J])(f: A => B): Out[B, J] =
          r.map(f)
      }

  }

}

