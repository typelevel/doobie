package doobie
package world

import doobie.util._
import doobie.JdbcType
import java.sql.PreparedStatement
import scalaz._
import Scalaz._

object statement extends IndexedWorld {

  protected type R = PreparedStatement

  // A typeclass associating a Scala type A with a JDBC type J. Given this association we can define
  // actions in this world for setting the next parameter (or setting NULL). So rather than calling
  // set, setNull, etc., directly we use an instance of In to fix A and J.
  class In[A, J] private (val f: PreparedStatement => (Int, A) => Unit)(implicit val J: JdbcType[J]) {

    def set(a: A): Action[Unit] = 
      get >>= (n => next(f(_)(_, a)) :++> f"SET $n%d, ${J.name}%s => $a%s")

    def setNull: Action[Unit] = 
      get >>= (n => next(_.setNull(_, J.toInt)) :++> f"SET $n%d, ${J.name}%s => NULL")

    def contramap[B](g: B => A): In[B, J] =
      new In[B, J](ps => (n, a) => f(ps)(n, g(a)))

  }

  object In {

    // For example, In[Int, DECIMAL](_.setInt)
    def apply[A, J: JdbcType](f: PreparedStatement => (Int, A) => Unit): In[A, J] = 
      new In[A, J](f)

    implicit def contravariant[J]: Contravariant[({type λ[α] = In[α, J]})#λ] =
      new Contravariant[({type λ[α] = In[α, J]})#λ] {
        def contramap[A, B](r: In[A, J])(f: B => A): In[B, J] =
          r.contramap(f)
      }

  }

  def execute: Action[Unit] =
    asks(_.execute).void

  def executeUpdate: Action[Int] =
    asks(_.executeUpdate)

}


