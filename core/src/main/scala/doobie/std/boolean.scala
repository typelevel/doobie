package doobie.std

import doobie.free.preparedstatement.setBoolean
import doobie.free.resultset.{ getBoolean, updateBoolean }
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object boolean {

  implicit val BooleanAtom: Unlifted[Boolean] = 
    Unlifted.create(jdbctype.Boolean, setBoolean, updateBoolean, getBoolean)

}