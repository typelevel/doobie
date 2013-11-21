package doobie
package world

import doobie.JdbcType
import scalaz.Contravariant
import java.sql.{PreparedStatement => PS}

object StatementWorld extends IndexedWorld[PS] {

  class In[A, J] private (f: PS => (Index, A) => Unit)(implicit J: JdbcType[J]) {

    def set(a: A): Action[Unit] = 
      next(f(_)(_, a))

    def setNull: Action[Unit] = 
      next(_.setNull(_, J.toInt))

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


