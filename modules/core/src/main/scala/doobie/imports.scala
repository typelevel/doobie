package doobie

import doobie.syntax.AllSyntax

/** Module of aliases for commonly-used types and syntax; use as `import doobie.imports._` */
object imports extends AllSyntax with doobie.hi.Aliases {


  /** @group Type Aliases */      type Meta[A] = doobie.util.meta.Meta[A]
  /** @group Companion Aliases */ val  Meta    = doobie.util.meta.Meta

  /** @group Type Aliases */      type Composite[A] = doobie.util.composite.Composite[A]
  /** @group Companion Aliases */ val  Composite    = doobie.util.composite.Composite

  /** @group Type Aliases */      type Query[A,B] = doobie.util.query.Query[A,B]
  /** @group Companion Aliases */ val  Query      = doobie.util.query.Query

  /** @group Type Aliases */      type Update[A] = doobie.util.update.Update[A]
  /** @group Companion Aliases */ val  Update    = doobie.util.update.Update

  /** @group Type Aliases */      type Query0[A]  = doobie.util.query.Query0[A]
  /** @group Companion Aliases */ val  Query0     = doobie.util.query.Query0

  /** @group Type Aliases */      type Update0   = doobie.util.update.Update0
  /** @group Companion Aliases */ val  Update0   = doobie.util.update.Update0

  /** @group Type Aliases */      type SqlState = doobie.enum.sqlstate.SqlState
  /** @group Companion Aliases */ val  SqlState = doobie.enum.sqlstate.SqlState

  /** @group Type Aliases */      type Param[A] = doobie.util.param.Param[A]
  /** @group Companion Aliases */ val  Param    = doobie.util.param.Param

  /** @group Type Aliases */      type Transactor[M[_]] = doobie.util.transactor.Transactor[M]
  /** @group Type Aliases */      val  Transactor          = doobie.util.transactor.Transactor

  /** @group Type Aliases */      type LogHandler = doobie.util.log.LogHandler
  /** @group Companion Aliases */ val  LogHandler = doobie.util.log.LogHandler

  /** @group Type Aliases */      type Fragment = doobie.util.fragment.Fragment
  /** @group Companion Aliases */ val  Fragment = doobie.util.fragment.Fragment

  /** @group Type Aliases */      type KleisliInterpreter[F[_]] = doobie.free.KleisliInterpreter[F]
  /** @group Companion Aliases */ val  KleisliInterpreter       = doobie.free.KleisliInterpreter

  /** @group Companion Aliases */ val  Fragments = doobie.util.fragments

}
