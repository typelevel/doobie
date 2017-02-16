package doobie

import eu.timepit.refined.api.RefType

package object refined {

  import eu.timepit.refined.api.{Refined, Validate}
  import eu.timepit.refined.auto._
  import eu.timepit.refined._
  import scala.reflect.runtime.universe.TypeTag

  import doobie.imports._
  import doobie.util.invariant._

  implicit def refinedAtom[T, P, F[_,_]](implicit atomT: Atom[T], v: Validate[T, P], refType: RefType[F], tag: Manifest[F[T,P]]): Atom[F[T, P]] = {
#+scalaz
    atomT.xmap[F[T,P]](
      refineType[T,P,F],
      unwrapRefinedType[T,P,F]
    )
#-scalaz
#+cats
  atomT.imap[F[T,P]](refineType[T,P,F])(unwrapRefinedType[T,P,F])
#-cats
  }

  implicit def refinedAtomOption[T, P, F[_,_]](implicit atomT: Atom[Option[T]], v: Validate[T, P], refType: RefType[F], tag: Manifest[F[T,P]]): Atom[Option[F[T, P]]] = {
#+scalaz
    atomT.xmap[Option[F[T,P]]](
      _.map[F[T,P]](refineType[T,P,F]),
      _.map(unwrapRefinedType[T,P,F])
    )
#-scalaz
#+cats
  atomT.imap[Option[F[T,P]]](_.map[F[T,P]](refineType[T,P,F]))(_.map(unwrapRefinedType[T,P,F]))
#-cats
  }

#+scalaz
  import scalaz.InvariantFunctor
  import scalaz.syntax.applicative._
#-scalaz
#+cats
  import cats.functor.{ Invariant => InvariantFunctor }
  import cats.implicits._
#-cats

  implicit def refinedComposite[T, P, F[_,_]](implicit compositeT: Composite[T], v: Validate[T, P], refType: RefType[F], manifest: Manifest[F[T,P]]): Composite[F[T,P]] =
#+scalaz
    compositeT.xmap[F[T,P]](
      refineType[T,P,F],
      unwrapRefinedType[T,P,F]
    )
#-scalaz
#+cats
      compositeT.imap[F[T,P]](refineType[T,P,F])(unwrapRefinedType[T,P,F])
#-cats

  def refineType[T,P,F[_,_]](t: T)(implicit refType: RefType[F], v: Validate[T, P], tag: Manifest[F[T,P]]): F[T,P] =
    rightOrException[F[T,P]](refType.refine[P](t)(v))

  def unwrapRefinedType[T,P,F[_,_]](ftp: F[T,P])(implicit refType: RefType[F]): T =
    refType.unwrap(ftp)

  def rightOrException[T](either: Either[String, T])(implicit ev: Manifest[T]): T = either match {
    case Left(err) => throw new SecondaryValidationFailed[T](err)(ev)
    case Right(t) => t
  }
}
