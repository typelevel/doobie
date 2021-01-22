// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import scala.reflect.macros.blackbox

sealed trait Unary[T]

object Unary {
  object Instance extends Unary[Any]

  def apply[T](implicit ev: Unary[T]): Unary[T] = ev

  def impl[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Unary[T]] = {
    import c.universe._
    import definitions._

    val t = weakTypeOf[T]

    def primitives = Predef.Set(DoubleTpe, FloatTpe, ShortTpe, ByteTpe, IntTpe, LongTpe, CharTpe, BooleanTpe, UnitTpe)

    val isValueClass = t <:< AnyValTpe && !primitives.exists(_ =:= t)

    val params = t.decls.collect(
      if (isValueClass) { case p: TermSymbol if p.isParamAccessor && p.isMethod => p }
      else { case p: TermSymbol if p.isCaseAccessor && !p.isMethod => p }
    )

    if (params.size != 1) {
      c.abort(c.enclosingPosition, s"${t.typeSymbol.name} is not a unary type")
    }

    c.Expr[Unary[T]](q"_root_.doobie.util.Unary.Instance.asInstanceOf[_root_.doobie.util.Unary[${t.resultType}]]")
  }

  implicit def gen[T]: Unary[T] = macro impl[T]
}
