package doobie.std

import doobie.free.preparedstatement.setString
import doobie.free.resultset.{updateString, getString}
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object string {

  implicit val StringAtom: Atom[String] = 
    Atom.atom(jdbctype.VarChar, setString, updateString, getString)

}