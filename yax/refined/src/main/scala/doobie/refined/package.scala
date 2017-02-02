package doobie

package object refined {

  import eu.timepit.refined.api.{Refined, Validate}
  import eu.timepit.refined.auto._
  import eu.timepit.refined._
  import scala.reflect.runtime.universe.TypeTag

  import doobie.imports._

  implicit def refinedMeta[T, P](implicit metaT: Meta[T], v: Validate[T, P], tag: TypeTag[T Refined P]): Meta[T Refined P] =
    metaT.xmap[T Refined P](
      refineV[P](_)(v).right.get,
      _.value
    )

#+scalaz
  import scalaz.InvariantFunctor
  import scalaz.syntax.applicative._
#-scalaz
#+cats
  import cats.functor.{ Invariant => InvariantFunctor }
  import cats.implicits._
#-cats

  implicit def refinedComposite[T, P](implicit compositeT: Composite[T], v: Validate.Plain[T, P], tag: TypeTag[T Refined P]): Composite[T Refined P] =
#+scalaz
      compositeT.xmap[T Refined P](
        refineV[P](_)(v).right.get,
        _.value
      )
#-scalaz
#+cats
      compositeT.imap[T Refined P](refineV[P](_)(v).right.get)(_.value)
#-cats

}
