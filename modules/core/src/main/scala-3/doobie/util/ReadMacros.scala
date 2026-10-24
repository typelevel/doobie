// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import scala.annotation.implicitNotFound
import scala.deriving.Mirror
import scala.quoted.*

private[util] object ReadMacros:

  /** @param autoDerivation
    *   Whether to derive Read instances for case classes, which Read.Auto's implicit enables
    */
  def instance[A: Type](autoDerivation: Boolean)(using Quotes): Expr[Read[A]] =
    val resolver = new Resolver(autoDerivation, fieldsViaAutoDerivedRead = false)
    // The compiler only picks Read.instance when there is no more specific Read[A] in scope. But Read.Auto's implicit
    // is imported, so the compiler finds it before any instances in implicit scope (e.g. companion objects).
    if autoDerivation then resolver.nested[A](Nil) else resolver.builtin[A](Nil)

  def derived[A: Type](using Quotes): Expr[Read[A]] =
    // Whether automatic derivation is enabled isn't known here, so fields are resolved through Read.Auto's implicit
    // if it's in scope
    new Resolver(autoDerivation = false, fieldsViaAutoDerivedRead = true).product[A](Nil, explicit = true)

  private class Resolver(autoDerivation: Boolean, fieldsViaAutoDerivedRead: Boolean)(using val quotes: Quotes):
    import quotes.reflect.*

    /** Products being derived (innermost first) and the label of the field being resolved in each */
    private type Trail = List[(TypeRepr, String)]

    /** Our own implicits, which would otherwise be found by every search for a Read instance */
    private val ignored: List[Symbol] =
      TypeRepr.of[Read.type].typeSymbol.methodMember("instance") ++
        (if fieldsViaAutoDerivedRead then Nil else TypeRepr.of[Read.Auto].typeSymbol.methodMember("autoDerivedRead"))

    /** Read instance for a field or element of the type being constructed. Instances in scope take priority. */
    def nested[T: Type](trail: Trail): Expr[Read[T]] =
      (Implicits.searchIgnoring(TypeRepr.of[Read[T]])(ignored*): @unchecked) match
        case found: ImplicitSearchSuccess => found.tree.asExprOf[Read[T]]
        case _: NoMatchingImplicits       => builtin[T](trail)
        // e.g. ambiguous instances, or an instance whose macro failed
        case failure: ImplicitSearchFailure => report.errorAndAbort(failure.explanation)

    def builtin[T: Type](trail: Trail): Expr[Read[T]] =
      Type.of[T] match
        case '[Option[a]] if TypeRepr.of[T] =:= TypeRepr.of[Option[a]] =>
          '{ ${ nested[a](trail) }.toOpt }.asExprOf[Read[T]]
        case _ =>
          single[T].getOrElse(product[T](trail, explicit = false))

    private def single[T: Type]: Option[Expr[Read[T]]] =
      Implicits.search(TypeRepr.of[Get[T]]) match
        case found: ImplicitSearchSuccess  => Some('{ new Read.Single[T](${ found.tree.asExprOf[Get[T]] }) })
        case ambiguous: AmbiguousImplicits => report.errorAndAbort(ambiguous.explanation)
        case _                             => None

    /** @param explicit
      *   Whether derivation was requested via Read.derived, rather than being a fallback
      */
    def product[T: Type](trail: Trail, explicit: Boolean): Expr[Read[T]] =
      val tpe = TypeRepr.of[T]
      val isTuple = tpe <:< TypeRepr.of[Tuple]
      Expr.summon[Mirror.ProductOf[T]] match
        case Some('{
              $m: Mirror.ProductOf[T] {
                type MirroredElemTypes = elemTypes; type MirroredElemLabels = elemLabels
              }
            }) if explicit || isTuple || autoDerivation =>
          if trail.exists((owner, _) => owner =:= tpe) then
            report.errorAndAbort(s"Cannot derive Read instance for recursive type ${tpe.show}")

          val reads = fieldReads[elemTypes, elemLabels](tpe, trail)
          if reads.isEmpty then
            report.errorAndAbort(
              s"Cannot derive Read instance for ${tpe.show}. Case objects and empty case classes are not supported (as they do not have any fields)")

          val composite = '{ new Read.CompositeOfInstances[Any](Array(${ Varargs(reads) }*)) }
          if isTuple then '{ $composite.map[T](values => Tuple.fromArray(values).asInstanceOf[T]) }
          else '{ $composite.map[T](values => $m.fromProduct(Tuple.fromArray(values))) }
        case _ if explicit =>
          report.errorAndAbort(s"Cannot derive Read instance for ${tpe.show} as it is not a case class or tuple")
        case _ =>
          report.errorAndAbort(notFoundMessage(tpe, trail))

    /** Read instances for each field, given a Mirror's element types and labels */
    private def fieldReads[Elems: Type, Labels: Type](owner: TypeRepr, trail: Trail): List[Expr[Read[Any]]] =
      (Type.of[Elems], Type.of[Labels]) match
        case ('[elem *: elems], '[label *: labels]) =>
          val fieldLabel = Type.valueOfConstant[label].fold("?")(_.toString)
          val read = '{ ${ nested[elem]((owner, fieldLabel) :: trail) }.asInstanceOf[Read[Any]] }
          read :: fieldReads[elems, labels](owner, trail)
        case _ => Nil

    /** Read's @implicitNotFound message, with the fields that led to the missing instance */
    private def notFoundMessage(tpe: TypeRepr, trail: Trail): String =
      val template = TypeRepr.of[Read].typeSymbol.getAnnotation(TypeRepr.of[implicitNotFound].typeSymbol) match
        case Some(Apply(_, List(Literal(StringConstant(message))))) => message
        case _ => "Cannot find or construct a Read instance for type: ${A}"
      val requiredBy = trail.map((owner, label) => s"\n  (required by field $label of ${owner.show})")
      template.replace("${A}", tpe.show + requiredBy.mkString)
