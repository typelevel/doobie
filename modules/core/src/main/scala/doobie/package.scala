// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

/**
 * Top-level import, providing aliases for the most commonly used types and modules from
 * doobie-free and doobie-core. A typical starting set of imports would be something like this.
 * {{{
 * import cats.implicits._
 * import doobie._, doobie.implicits._
 * }}}
 * @see The [[http://tpolecat.github.io/doobie/ doobie microsite]] for much more information.
 */
package object doobie
  extends Aliases
     with doobie.hi.Modules
     with doobie.free.Modules
     with doobie.free.Types {

  /** Top-level import for all instances and syntax provided by doobie-free and doobie-core. */
  object implicits
    extends free.Instances
       with syntax.AllSyntax
       with util.meta.LegacyMeta

}
