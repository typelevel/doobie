package doobie.std

import java.net.URL

import doobie.free.preparedstatement.setURL
import doobie.free.resultset.getURL
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object url {

  implicit val URLAtom: Atom[URL] = 
    Atom.atom(jdbctype.DataLink, setURL, getURL)

}