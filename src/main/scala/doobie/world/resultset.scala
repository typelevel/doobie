package doobie
package world

import doobie.util._
import doobie.JdbcType
import java.sql.ResultSet
import scalaz._
import Scalaz._

object resultset extends IndexedWorld {

  protected type R = ResultSet

  sealed class Out[A, J] private (f: ResultSet => Int => A)(implicit J: JdbcType[J]) { 

    def get: Action[A] = 
      next(f(_)(_))

    def map[B](g: A => B): Out[B, J] =
      new Out[B, J](rs => n => g(f(rs)(n)))

  }

  object Out {

    def apply[A, J: JdbcType](f: ResultSet => Int => A): Out[A, J] = new Out[A, J](f)

    implicit def functor[J]: Functor[({type λ[α] = Out[α, J]})#λ] =
      new Functor[({type λ[α] = Out[α, J]})#λ] {
        def map[A, B](r: Out[A, J])(f: A => B): Out[B, J] =
          r.map(f)
      }

  }

}

