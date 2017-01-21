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

}
