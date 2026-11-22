// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import scala.collection.mutable
import scala.deriving.Mirror
import scala.quoted.*

/** Macro implementation of Write derivation (Write.derived and automatic derivation).
  *
  * The whole instance is built in one macro expansion. Every distinct type involved gets a single `val` in the
  * generated block, which is then referenced wherever that type appears, so the size of the generated code grows with
  * the number of distinct types rather than the number of paths through the type.
  *
  * Instances for each type are resolved in this order:
  *   1. An instance found by implicit search, ignoring the structural instances below (custom Write instances, Write
  *      from Put, etc.)
  *   1. Option: the Write of the element type, made nullable (what `optionalFromWrite` does)
  *   1. Tuples: derived from the Write of each element (what `tupleBase` / `tuple` do)
  *   1. Case classes: derived, but only for automatic derivation. `Write.derived` requires them to have an instance.
  *   1. Anything else: plain implicit search, which may produce an instance outside the shared cache.
  *
  * The structural instances are ignored in the initial search because a nested search they trigger can't be ignored,
  * and it would derive nested case classes through the automatic derivation given in a separate macro expansion.
  */
private[util] object WriteMacros:

  def derived[A: Type](using Quotes): Expr[Write[A]] =
    new Derivation(autoDerivation = false).deriveRoot[A]

  def autoDerived[A: Type](using Quotes): Expr[Derived[Write[A]]] =
    '{ Derived(${ new Derivation(autoDerivation = true).deriveRoot[A] }) }

  private final class Derivation(autoDerivation: Boolean)(using val quotes: Quotes):
    import quotes.reflect.*

    private val structuralInstances: List[Symbol] =
      val companion = TypeRepr.of[Write.type].typeSymbol
      List("fromDerived", "optionalFromWrite", "tuple", "tupleBase").map { name =>
        companion.methodMember(name) match
          case List(sym) => sym
          case other     => report.errorAndAbort(s"doobie internal error: expected one Write.$name, found ${other.size}")
      }

    private val cache = mutable.ListBuffer.empty[(TypeRepr, Symbol)]
    private val valDefs = mutable.ListBuffer.empty[ValDef]
    private val inProgress = mutable.ListBuffer.empty[TypeRepr]
    private var rootName = ""

    def deriveRoot[A: Type]: Expr[Write[A]] =
      rootName = Type.show[A]
      // The root is always derived. Summoning it could find the instance currently being defined
      // (e.g. `given Write[Foo] = Write.derived`).
      val rootPath = TypeRepr.of[A].typeSymbol.name
      val root = withCycleCheck[A](rootPath)(product[A](rootPath).getOrElse(
        report.errorAndAbort(s"Cannot derive Write instance for ${Type.show[A]} as it is not a case class or tuple")))
      Block(valDefs.toList, root).asExprOf[Write[A]]

    /** A reference to the shared instance for T, resolving and caching it on first use. */
    private def resolve[T: Type](path: String): Term =
      val tpe = TypeRepr.of[T]
      cache.collectFirst { case (cached, sym) if cached =:= tpe => Ref(sym) }.getOrElse {
        val instance = withCycleCheck[T](path)(searchDirect[T].getOrElse(assemble[T](path)))
        val sym = Symbol.newVal(
          Symbol.spliceOwner,
          s"derivedWrite${cache.size}",
          TypeRepr.of[Write[T]],
          Flags.EmptyFlags,
          Symbol.noSymbol)
        valDefs += ValDef(sym, Some(instance.changeOwner(sym)))
        cache += (tpe -> sym)
        Ref(sym)
      }

    private def searchDirect[T: Type]: Option[Term] =
      Implicits.searchIgnoring(TypeRepr.of[Write[T]])(structuralInstances*) match
        case success: ImplicitSearchSuccess => Some(success.tree)
        case _                              => None

    private def assemble[T: Type](path: String): Term =
      Type.of[T] match
        case '[Option[t]] =>
          val elem = resolve[t](path).asExprOf[Write[t]]
          '{ $elem.toOpt }.asTerm
        case _ if TypeRepr.of[T] <:< TypeRepr.of[Tuple] =>
          product[T](path).getOrElse(searchFallback[T](path))
        case _ if autoDerivation =>
          product[T](path).getOrElse(searchFallback[T](path))
        case _ =>
          searchFallback[T](path)

    /** A Composite of each field's instance, or None if T is not a product type. */
    private def product[T: Type](path: String): Option[Term] =
      Expr.summon[Mirror.ProductOf[T]].map {
        case '{ $_ : Mirror.ProductOf[T] { type MirroredElemTypes = elems; type MirroredElemLabels = labels } } =>
          val fields = elemTypes[elems].zip(elemLabels[labels])
          if fields.isEmpty then
            report.errorAndAbort(
              "Cannot derive Write instance for case objects or empty case classes (as they do not have any fields)")
          val fieldWrites = Expr.ofList(fields.map { (fieldType, label) =>
            fieldType match
              case '[f] => resolve[f](s"$path.$label").asExprOf[Write[?]]
              case _    => unexpectedShape[T](s"field $label")
          })
          '{ new Write.Composite[T]($fieldWrites, a => a.asInstanceOf[Product].productIterator.toList) }.asTerm
        case _ => unexpectedShape[T]("Mirror")
      }

    private def searchFallback[T: Type](path: String): Term =
      Implicits.search(TypeRepr.of[Write[T]]) match
        case success: ImplicitSearchSuccess => success.tree
        case _                              =>
          val hint =
            if autoDerivation then "a Write or Put instance is in scope for it"
            else "a Write or Put instance is in scope for it (e.g. via Write.derived), or use automatic derivation"
          report.errorAndAbort(
            s"Cannot derive Write instance for $rootName: no Write instance found for $path (of type ${Type.show[T]}). " +
              s"Ensure that $hint.")

    private def withCycleCheck[T: Type](path: String)(body: => Term): Term =
      val tpe = TypeRepr.of[T]
      if inProgress.exists(_ =:= tpe) then
        report.errorAndAbort(
          s"Cannot derive Write instance for $rootName: ${Type.show[T]} at $path refers to itself, which would require an unbounded number of columns.")
      inProgress += tpe
      try body
      finally inProgress -= tpe

    private def elemTypes[T: Type]: List[Type[?]] =
      Type.of[T] match
        case '[EmptyTuple] => Nil
        case '[h *: t]     => Type.of[h] :: elemTypes[t]
        case _             => unexpectedShape[T]("MirroredElemTypes")

    private def elemLabels[T: Type]: List[String] =
      Type.of[T] match
        case '[EmptyTuple] => Nil
        case '[h *: t]     =>
          TypeRepr.of[h] match
            case ConstantType(StringConstant(label)) => label :: elemLabels[t]
            case _                                   => "_" :: elemLabels[t]
        case _ => unexpectedShape[T]("MirroredElemLabels")

    private def unexpectedShape[T: Type](what: String): Nothing =
      report.errorAndAbort(s"doobie internal error: unexpected $what shape for ${Type.show[T]}")
