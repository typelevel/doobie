package doobie.std

import java.sql.RowId

import doobie.free.preparedstatement.setRowId
import doobie.free.resultset.{ updateRowId, getRowId }
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

object rowid {

  implicit val RowIdAtom: Unlifted[RowId] = 
    Unlifted.create(jdbctype.RowId, setRowId, updateRowId, getRowId)

}