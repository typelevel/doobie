package doobie.postgres.hi

trait Modules {
  lazy val PHPC  = pgconnection
  lazy val PHC   = connection
  lazy val PHLO  = largeobject
  lazy val PHLOM = largeobjectmanager
}
