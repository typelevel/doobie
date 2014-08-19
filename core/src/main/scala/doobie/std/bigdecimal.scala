package doobie.std

import doobie.free.preparedstatement.setBigDecimal
import doobie.free.resultset.{ getBigDecimal, updateBigDecimal }
import doobie.util.atom.Unlifted
import doobie.enum.jdbctype

import scalaz.syntax.functor._

object bigdecimal {

  implicit val JavaBigDecimalAtom: Unlifted[java.math.BigDecimal] =
    Unlifted.create(jdbctype.Numeric, setBigDecimal, updateBigDecimal, getBigDecimal)

  implicit val BigDecimalAtom: Unlifted[BigDecimal] =
    JavaBigDecimalAtom.xmap(BigDecimal(_), _.bigDecimal)

}