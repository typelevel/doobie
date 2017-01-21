package doobie

package object refined {

  import scala.reflect.runtime.universe.TypeTag

  implicit def refinedMeta[T, P](implicit metaT: Meta[T], v: Validate[T, P], tag: TypeTag[T Refined P]): Meta[T Refined P] =
    metaT.xmap[T Refined P](
      refineV[P](_)(v).right.get,
      _.value
    )

}
