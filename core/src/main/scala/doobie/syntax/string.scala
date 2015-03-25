package doobie.syntax

import doobie.util.atom._
import doobie.util.composite._
import doobie.util.query._
import doobie.util.update._
import doobie.syntax.process._

import doobie.hi._

import scalaz.Monad
import scalaz.syntax.monad._
import scalaz.stream.Process

import shapeless._

/** String interpolator for SQL literals. */
object string {

  implicit class SqlInterpolator(val sc: StringContext) {

    val stackFrame = {
      import Predef._
      Thread.currentThread.getStackTrace.lift(3)
    }

    val rawSql = sc.parts.mkString("?")

    class Builder[A: Composite](a: A) {
      def query[O: Composite]: Query0[O] = Query[A, O](rawSql, stackFrame).toQuery0(a)
      def update: Update0 = Update[A](rawSql, stackFrame).toUpdate0(a)
    }

    object sql extends ProductArgs {
      def applyProduct[A: Composite](a: A): Builder[A] = new Builder(a)
    }
  }
}
