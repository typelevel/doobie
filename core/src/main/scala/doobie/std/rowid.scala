package doobie.std

import java.sql.RowId

import doobie.free.preparedstatement.setRowId
import doobie.free.resultset.{ updateRowId, getRowId }
import doobie.util.atom.Atom
import doobie.enum.jdbctype

object rowid {

  implicit val RowIdAtom: Atom[RowId] = 
    Atom.atom(jdbctype.RowId, setRowId, updateRowId, getRowId)

}