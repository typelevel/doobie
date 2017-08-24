package doobie

package object postgres
  extends postgres.free.Types
     with postgres.free.Modules
     with postgres.hi.Modules {

  object implicits
    extends Instances
       with free.Instances
       with syntax.ToPostgresMonadErrorOps

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
