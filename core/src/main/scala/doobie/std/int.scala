package doobie.std

import doobie.free.preparedstatement.setInt
import doobie.free.resultset.{ getInt, updateInt }
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object int {

  implicit val IntAtom: Unlifted[Int] = 
    Unlifted.create(jdbctype.Integer, setInt, updateInt, getInt)

}