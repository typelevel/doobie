package doobie
package world

import doobie.JdbcType
import java.sql.PreparedStatement
import scalaz._
import Scalaz._

object StatementWorld extends IndexedWorld {

  protected type R = PreparedStatement

  /** Typeclass for types that can be IN parameters. */
  class In[A, J] private (f: PreparedStatement => (Int, A) => Unit)(implicit J: JdbcType[J]) {

    def set(a: A): Action[Unit] = 
      get >>= (n => next(f(_)(_, a)) :++> f"SET $n%d, ${J.name}%s => $a%s")

    def setNull: Action[Unit] = 
      get >>= (n => next(_.setNull(_, J.toInt)) :++> f"SET $n%d, ${J.name}%s => NULL")

    def contramap[B](g: B => A): In[B, J] =
      new In[B, J](ps => (n, a) => f(ps)(n, g(a)))

  }

  object In {

    /** Construct an `In` by providing a JDBC type and the appropriate setter. */
    def apply[A, J: JdbcType](f: PreparedStatement => (Int, A) => Unit): In[A, J] = 
      new In[A, J](f)

    implicit def contravariant[J]: Contravariant[({type λ[α] = In[α, J]})#λ] =
      new Contravariant[({type λ[α] = In[α, J]})#λ] {
        def contramap[A, B](r: In[A, J])(f: B => A): In[B, J] =
          r.contramap(f)
      }

  }

}


