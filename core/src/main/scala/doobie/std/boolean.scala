package doobie.std

import doobie.free.preparedstatement.setBoolean
import doobie.free.resultset.{ getBoolean, updateBoolean }
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object boolean {

  implicit val BooleanAtom: Atom[Boolean] = 
    Atom.atom(jdbctype.Boolean, setBoolean, updateBoolean, getBoolean)

}