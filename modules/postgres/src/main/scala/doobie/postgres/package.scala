// Copyright (c) 2013-2017 Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

package object postgres
  extends postgres.free.Types
     with postgres.free.Modules
     with postgres.hi.Modules {

  object implicits
    extends Instances
       with free.Instances
       with syntax.ToPostgresMonadErrorOps
       with syntax.ToCsvOps
       with syntax.ToFragmentOps

  object pgisimplicits
    extends PgisInstances

  @deprecated(message = "import doobie.postgres._, doobie.postgres.implicits._", since = "0.5.0")
  object imports
  extends postgres.free.Types
     with postgres.free.Modules
     with postgres.hi.Modules
     with Instances
     with free.Instances
     with syntax.ToPostgresMonadErrorOps {

    val sqlstate = doobie.postgres.sqlstate

    @deprecated(message = "import doobie.postgres.pgisimplicits._", since = "0.5.0")
    val pgistypes: pgisimplicits.type =
      pgisimplicits

  }

}
