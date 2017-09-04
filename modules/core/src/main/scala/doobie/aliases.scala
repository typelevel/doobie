// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

trait Aliases extends Types with Modules

trait Types {
  type Meta[A]                  = doobie.util.meta.Meta[A]
  type Composite[A]             = doobie.util.composite.Composite[A]
  type Query[A,B]               = doobie.util.query.Query[A,B]
  type Update[A]                = doobie.util.update.Update[A]
  type Query0[A]                = doobie.util.query.Query0[A]
  type Update0                  = doobie.util.update.Update0
  type SqlState                 = doobie.enum.SqlState
  type Param[A]                 = doobie.util.param.Param[A]
  type Transactor[M[_]]         = doobie.util.transactor.Transactor[M]
  type LogHandler               = doobie.util.log.LogHandler
  type Fragment                 = doobie.util.fragment.Fragment
  type KleisliInterpreter[F[_]] = doobie.free.KleisliInterpreter[F]
}

trait Modules {
  val  Meta               = doobie.util.meta.Meta
  val  Composite          = doobie.util.composite.Composite
  val  Query              = doobie.util.query.Query
  val  Update             = doobie.util.update.Update
  val  Query0             = doobie.util.query.Query0
  val  Update0            = doobie.util.update.Update0
  val  SqlState           = doobie.enum.SqlState
  val  Param              = doobie.util.param.Param
  val  Transactor         = doobie.util.transactor.Transactor
  val  LogHandler         = doobie.util.log.LogHandler
  val  Fragment           = doobie.util.fragment.Fragment
  val  KleisliInterpreter = doobie.free.KleisliInterpreter
  val  Fragments          = doobie.util.fragments
}
