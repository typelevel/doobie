package doobie.std

import doobie.free.preparedstatement.setByte
import doobie.free.resultset.{ getByte, updateByte }
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object byte {

  implicit val ByteAtom: Unlifted[Byte] = 
    Unlifted.create(jdbctype.TinyInt, setByte, updateByte, getByte)

}