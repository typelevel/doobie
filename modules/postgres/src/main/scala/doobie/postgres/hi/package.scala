package doobie.postgres

package object hi
  extends Modules
     with free.Modules
     with free.Types {
  object implicits extends free.Instances
}
