package doobie.std

import doobie.free.preparedstatement.setFloat
import doobie.free.resultset.{ getFloat, updateFloat }
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object float {

  implicit val FloatAtom: Atom[Float] = 
    Atom.atom(jdbctype.Real, setFloat, updateFloat, getFloat)

}