package doobie
package world

import doobie.JdbcType
import java.sql.{PreparedStatement => PS}
import scalaz._
import Scalaz._

object StatementWorld extends IndexedWorld {

  type R = PS

  def W = implicitly[Monoid[W]]

  implicit class RunnableAction[A](a: Action[A]) {
    def unsafeRun(r: PS) = 
      run(State(r, Vector(), 1), a)
  }

  class In[A, J] private (f: PS => (Index, A) => Unit)(implicit J: JdbcType[J]) {

    def set(a: A): Action[Unit] = 
      for {
        n <- get
        a <- next(f(_)(_, a)) :++> f"SET $n%d, ${J.name}%s => $a%s"
      } yield a

    def setNull: Action[Unit] = 
      for {
        n <- get
        a <- next(_.setNull(_, J.toInt)) :++> f"SET $n%d, ${J.name}%s => NULL"
      } yield a

    def contramap[B](g: B => A): In[B, J] =
      new In[B, J](ps => (n, a) => f(ps)(n, g(a)))

  }

  object In {

    def apply[A, J: JdbcType](f: PS => (Index, A) => Unit): In[A, J] = new In[A, J](f)

    implicit def contravariant[J]: Contravariant[({type λ[α] = In[α, J]})#λ] =
      new Contravariant[({type λ[α] = In[α, J]})#λ] {
        def contramap[A, B](r: In[A, J])(f: B => A): In[B, J] =
          r.contramap(f)
      }

  }

}


