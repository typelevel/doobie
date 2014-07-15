package doobie.std

import doobie.free.preparedstatement.setByte
import doobie.free.resultset.getByte
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object byte {

  implicit val ByteAtom: Atom[Byte] = 
    Atom.atom(jdbctype.TinyInt, setByte, getByte)

}