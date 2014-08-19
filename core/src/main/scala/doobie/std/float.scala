package doobie.std

import doobie.free.preparedstatement.setFloat
import doobie.free.resultset.{ getFloat, updateFloat }
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object float {

  implicit val FloatAtom: Unlifted[Float] = 
    Unlifted.create(jdbctype.Real, setFloat, updateFloat, getFloat)

}