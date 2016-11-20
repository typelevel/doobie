package doobie.util

import doobie.util.composite.Composite
import doobie.util.query.{ Query, Query0 }
import doobie.util.update.{ Update, Update0 }
import shapeless.HNil

#+scalaz
import scalaz.Monoid
#-scalaz
#+cats
import cats.Monoid
#-cats

/** Module defining the `Fragment` data type. */
object fragment {

  /**
   * A statement fragment, which may include interpolated values. Fragments can be composed by
   * concatenation, which maintains the correct offset and mappings for interpolated values. Once
   * constructed a `Fragment` is opaque; it has no externally observable properties. Fragments are
   * eventually used to construct a [[Query0]] or [[Update0]].
   */
  sealed trait Fragment { fa =>

    protected type A                // type of interpolated argument (existential, HNil for none)
    protected def a: A              // the interpolated argument itself
    protected def ca: Composite[A]  // proof that we can map the argument to parameters
    protected def sql: String       // snipped of SQL with `ca.length` placeholders

    /** Compose this fragment with another, yielding a larger fragment. */
    def +(fb: Fragment): Fragment =
      new Fragment {
        type A  = (fa.A, fb.A)
        val ca  = fa.ca zip fb.ca
        val a   = (fa.a, fb.a)
        val sql = fa.sql + fb.sql
      }

    /** Construct a [[Query0]] from this fragment, with asserted row type `B`. */
    def query[B](implicit cb: Composite[B]): Query0[B] =
      Query[A, B](sql, None)(ca, cb).toQuery0(a)

    /** Construct an [[Update0]] from this fragment. */
    def update: Update0 =
      Update[A](sql, None)(ca).toUpdate0(a)

    override def toString =
      s"""Fragment("$sql")"""

  }

  object Fragment {

    /**
     * Construct a statement fragment with the given SQL string, which must contain sufficient `?`
     * placeholders to accommodate the fields of the given interpolated value. This is normally
     * accomplished via the string interpolator rather than direct construction.
     */
    def apply[A0](sql0: String, a0: A0)(implicit ev: Composite[A0]): Fragment =
      new Fragment {
        type A  = A0
        val ca  = ev
        val a   = a0
        val sql = sql0
      }

    /**
     * Construct a statement fragment with no interpolated values; the passed SQL string must not
     * contain `?` placeholders. This is normally accomplished via the string interpolator rather
     * than direct construction.
     */
    def const(sql: String): Fragment =
      Fragment[HNil](sql, HNil)

    /** The empty fragment. Adding this to another fragment has no affect. */
    val empty: Fragment =
      const("")

    /** Statement fragments form a monoid. */
    implicit val FragmentMonoid: Monoid[Fragment] =
#+scalaz
        Monoid.instance(_ + _, empty)
#-scalaz
#+cats
      new Monoid[Fragment] {
        val empty = Fragment.empty
        def combine(a: Fragment, b: Fragment) = a + b
      }
#-cats

  }

}
