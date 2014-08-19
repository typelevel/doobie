package doobie.std

import doobie.free.preparedstatement.setDouble
import doobie.free.resultset.{ getDouble, updateDouble }
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object double {

  implicit val DoubleAtom: Unlifted[Double] = 
    Unlifted.create(jdbctype.Double, setDouble, updateDouble, getDouble)

}