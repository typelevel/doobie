package doobie.std

import java.sql.Timestamp

import doobie.free.preparedstatement.setTimestamp
import doobie.free.resultset.{updateTimestamp, getTimestamp}
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object timestamp {

  implicit val TimestampAtom: Unlifted[Timestamp] = 
    Unlifted.create(jdbctype.Timestamp, setTimestamp, updateTimestamp, getTimestamp)

}