package doobie
package param

import scalaz._
import Scalaz._
import doobie.world.statement.In
import doobie.world.resultset.Out

trait InOutParam[A] {

}

object InOutParam {

  def apply[A](implicit A: InOutParam[A]): InOutParam[A] = A

  // N.B. we have to build these up from primitives rather than just combining an InParam and an 
  // OutParam because we need to guarantee that the JdbcTypes all line up.
  implicit def inout[A,J](implicit I: In[A,J], O: Out[A,J]): InOutParam[A] =
    ???

}
