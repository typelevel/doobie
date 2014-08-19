package doobie.std

import java.sql.Time

import doobie.free.preparedstatement.setTime
import doobie.free.resultset.{updateTime, getTime}
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object time {

  implicit val TimeAtom: Unlifted[Time] = 
    Unlifted.create(jdbctype.Time, setTime, updateTime, getTime)

}