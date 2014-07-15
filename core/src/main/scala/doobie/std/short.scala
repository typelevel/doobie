package doobie.std

import doobie.free.preparedstatement.setShort
import doobie.free.resultset.getShort
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object short {

  implicit val ShortAtom: Atom[Short] = 
    Atom.atom(jdbctype.SmallInt, setShort, getShort)

}