package doobie

import doobie.world.StatementWorld.{ In,  Action => SAction }
import doobie.world.ResultSetWorld.{ Out, Action => RAction }
// import scalaz.InvariantFunctor

case class InOut[A, J: JdbcType](in: In[A, J], out: Out[A, J]) {

  def xmap[B](f: A => B, g: B => A): InOut[B, J] =
    InOut(in contramap g, out map f)

  def set(a:A): SAction[Unit] = 
    in.set(a)

  def setNull: SAction[Unit] = 
    in.setNull

  def get: RAction[A] = 
    out.get

}

object InOut {

  implicit def inout[A, J: JdbcType](implicit i: In[A,J], o: Out[A,J]): InOut[A,J] =
    InOut(i, o)

  // implicit def invariant[J]: InvariantFunctor[({type λ[α] = InOut[α,J]})#λ] =
  //   new InvariantFunctor[({type λ[α] = InOut[α,J]})#λ] {
  //     def xmap[A, B](ma: InOut[A, J], f: A => B, g: B => A): InOut[B, J] =
  //       ma.xmap(f, g)
  //   }

}

