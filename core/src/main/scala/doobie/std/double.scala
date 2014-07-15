package doobie.std

import doobie.free.preparedstatement.setDouble
import doobie.free.resultset.{ getDouble, updateDouble }
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object double {

  implicit val DoubleAtom: Atom[Double] = 
    Atom.atom(jdbctype.Double, setDouble, updateDouble, getDouble)

}