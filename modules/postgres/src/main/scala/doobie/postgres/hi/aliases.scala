package doobie.postgres.hi

trait Modules {
  val PHPC  = pgconnection
  val PHC   = connection
  val PHLO  = largeobject
  val PHLOM = largeobjectmanager
}
