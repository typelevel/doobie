// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres

import magnolia.{ReadOnlyCaseClass, Magnolia}

trait TextPlatform {
  this: Text.type =>

  type Typeclass[T] = Text[T]

  def combine[T](ctx: ReadOnlyCaseClass[Text, T]): Text[T] =
    instance { (t, sb) =>
      // cannot prove at compile-time with magnolia
      if (ctx.parameters.isEmpty) {
        sys.error(s"Sorry, ${ctx.typeName.short} doesnt work with `Text` because it has no columns")
      }

      ctx.parameters.zipWithIndex.foreach {
        case (param, idx) =>
          if (idx != 0) sb.append(Text.DELIMETER)
          param.typeclass.unsafeEncode(param.dereference(t), sb)
      }
    }

  implicit def generic[A]: Text[A] = macro Magnolia.gen[A]
}
