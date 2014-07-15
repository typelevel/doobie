package doobie.std

import java.sql.Timestamp

import doobie.free.preparedstatement.setTimestamp
import doobie.free.resultset.{updateTimestamp, getTimestamp}
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object timestamp {

  implicit val TimestampAtom: Atom[Timestamp] = 
    Atom.atom(jdbctype.Timestamp, setTimestamp, updateTimestamp, getTimestamp)

}