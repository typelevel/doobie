package doobie.std

import doobie.free.preparedstatement.setShort
import doobie.free.resultset.{updateShort, getShort}
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object short {

  implicit val ShortAtom: Unlifted[Short] = 
    Unlifted.create(jdbctype.SmallInt, setShort, updateShort, getShort)

}