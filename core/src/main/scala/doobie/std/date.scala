package doobie.std

import java.sql.Date

import doobie.free.preparedstatement.setDate
import doobie.free.resultset.{ getDate, updateDate }
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object date {

  implicit val DateAtom: Atom[Date] = 
    Atom.atom(jdbctype.Date, setDate, updateDate, getDate)

}