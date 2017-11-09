package doobie.postgres
package syntax

class CsvOps[A](val a: A) extends AnyVal {
  def csv(implicit ev: Csv[A]): String =
    ev.encode(a, '"', '"')
}

trait ToCsvOps {
  implicit def toCsvOps[A](a: A): CsvOps[A] =
    new CsvOps(a)
}

object csv extends ToCsvOps
