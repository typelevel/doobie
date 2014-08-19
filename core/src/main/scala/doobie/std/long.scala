package doobie.std

import doobie.free.preparedstatement.setLong
import doobie.free.resultset.{ updateLong, getLong}
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object long {

  implicit val LongAtom: Unlifted[Long] = 
    Unlifted.create(jdbctype.BigInt, setLong, updateLong, getLong)

}