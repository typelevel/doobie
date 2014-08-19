package doobie.std

import doobie.free.preparedstatement.setString
import doobie.free.resultset.{updateString, getString}
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object string {

  implicit val StringAtom: Unlifted[String] = 
    Unlifted.create(jdbctype.VarChar, setString, updateString, getString)

}