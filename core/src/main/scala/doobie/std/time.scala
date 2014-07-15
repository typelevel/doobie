package doobie.std

import java.sql.Time

import doobie.free.preparedstatement.setTime
import doobie.free.resultset.{updateTime, getTime}
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object time {

  implicit val TimeAtom: Atom[Time] = 
    Atom.atom(jdbctype.Time, setTime, updateTime, getTime)

}