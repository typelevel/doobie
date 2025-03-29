// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.deriving.Mirror
import scala.quoted.*
import scala.compiletime.summonAll

object WriteDerivation:
  inline def derived[A <: Product](using
      mirror: Mirror.ProductOf[A]
  ): Write[A] =
    ${ deriveImpl[A]('mirror) }

  private def deriveImpl[A <: Product: Type](mirror: Expr[Mirror.ProductOf[A]])(using q: Quotes): Expr[Write[A]] = {
    import q.reflect.*

    mirror match {
      case '{ $m: Mirror.ProductOf[A] { type MirroredElemTypes = EmptyTuple } } =>
        report.errorAndAbort(
          "Cannot derive Write instance for case objects or empty case classes (as they do not have any fields)")
      case '{ $m: Mirror.ProductOf[A] { type MirroredElemTypes = elemTypes } } =>
        '{
          val writesTuple: Tuple.Map[elemTypes & Tuple, Write] = summonAll[Tuple.Map[elemTypes & Tuple, Write]]

          Write.Composite[A](writesTuple.toList.asInstanceOf[List[Write[Any]]], a => a.productIterator.toList)
        }
    }
  }
