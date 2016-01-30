package doobie.contrib.refined

import eu.timepit.refined.api.{ RefType, Validate }
import doobie.util.meta.Meta
import doobie.util.composite.Composite

import scala.reflect.runtime.universe.TypeTag

object refinedinstances {

  private val oops = (err: String) =>
    throw new Exception(s"SQL object cannot be mapped to scala type since $err")

  implicit def refinedMeta[T: Meta, P, F[_, _]](implicit tt: TypeTag[F[T, P]], validate: Validate[T, P], refType: RefType[F]): Meta[F[T, P]] =
    Meta[T].xmap(refType.refine[P](_).fold(oops, identity), refType.unwrap)

  implicit def refinedComposite[T: Composite, P, F[_, _]](implicit tt: TypeTag[F[T, P]], validate: Validate[T, P], refType: RefType[F]): Composite[F[T, P]] =
    Composite[T].xmap(refType.refine[P](_).fold(oops, identity), refType.unwrap)

}