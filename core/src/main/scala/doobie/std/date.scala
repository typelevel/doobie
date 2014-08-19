package doobie.std

import java.sql.Date

import doobie.free.preparedstatement.setDate
import doobie.free.resultset.{ getDate, updateDate }
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object date {

  implicit val DateAtom: Unlifted[Date] = 
    Unlifted.create(jdbctype.Date, setDate, updateDate, getDate)

}