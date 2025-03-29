// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror
import scala.quoted.*
import scala.compiletime.summonAll

object ReadDerivation:
  inline def derived[A <: Product](using
      inline mirror: Mirror.ProductOf[A]
  ): Read[A] =
    ${ deriveImpl[A]('mirror) }

  private def deriveImpl[A <: Product: Type](mirror: Expr[Mirror.ProductOf[A]])(using q: Quotes): Expr[Read[A]] =
    import q.reflect.*

    mirror match
      case '{ $m: Mirror.ProductOf[A] { type MirroredElemTypes = EmptyTuple } } =>
        report.errorAndAbort(
          "Cannot derive Read instance for case objects or empty case classes (as they do not have any fields)")
      case '{ $m: Mirror.ProductOf[A] { type MirroredElemTypes = elemTypes } } =>
        '{
          val readsTuple: Tuple.Map[elemTypes & Tuple, Read] = summonAll[Tuple.Map[elemTypes & Tuple, Read]]
          Read.CompositeOfInstances[Any](readsTuple.productIterator.asInstanceOf[Iterator[Read[Any]]].toArray).map(a =>
            $m.fromProduct(Tuple.fromArray(a)))
        }
