package doobie.hi

import doobie.enum.holdability._
import doobie.enum.transactionisolation._

import doobie.syntax.catchable.ToDoobieCatchableOps._

import doobie.free.{ connection => C }
import doobie.free.{ drivermanager => DM }
import doobie.free.{ driver => D }

import java.sql.Savepoint

import scala.collection.immutable.Map
import scala.collection.JavaConverters._

import scalaz.syntax.id._
import scalaz.syntax.traverse._
import scalaz.std.list._

/**
 * Module of high-level constructors for `DriverManagerIO` actions. 
 * @group Modules
 */
object drivermanager {

  /** @group Typeclass Instances */
  implicit val CatchableDriverManagerIO = DM.CatchableDriverManagerIO

  /** @group Lifting */
  def delay[A](a: => A): DriverManagerIO[A] =
    DM.delay(a)

  /** @group Connections */
  def getConnection[A](url: String)(k: ConnectionIO[A]): DriverManagerIO[A] =
    DM.getConnection(url).flatMap(s => DM.lift(s, k ensuring C.close))

  /** @group Connections */
  def getConnection[A](url: String, user: String, password: String)(k: ConnectionIO[A]): DriverManagerIO[A] =
    DM.getConnection(url, user, password).flatMap(s => DM.lift(s, k ensuring C.close))

  /** @group Connections */
  def getConnection[A](url: String, props: Map[String, String])(k: ConnectionIO[A]): DriverManagerIO[A] = {
    val props0 = new java.util.Properties <| (_.putAll(props.asJava))
    DM.getConnection(url, props0).flatMap(s => DM.lift(s, k ensuring C.close))
  }

  /** @group Drivers */
  def getDriver[A](a: String)(k: DriverIO[A]): DriverManagerIO[A] =
    DM.getDriver(a).flatMap(s => DM.lift(s, k))

  /** @group Drivers */
  def getDrivers[A](k: DriverIO[A]): DriverManagerIO[List[A]] = {
    def enumToList[B](e: java.util.Enumeration[B]): List[B] = {
      def go(e: java.util.Enumeration[B], accum: List[B]): List[B] =
        e.hasMoreElements match {
          case true  => go(e, e.nextElement :: accum)
          case false => accum.reverse
        }
      go(e, Nil)
    }
    DM.getDrivers.flatMap(ds => enumToList(ds).traverse(DM.lift(_, k)))
  }

  /** @group Properties */
  val getLoginTimeout: DriverManagerIO[Int] =
    DM.getLoginTimeout

  /** @group Properties */
  def setLoginTimeout(a: Int): DriverManagerIO[Unit] =
    DM.setLoginTimeout(a)

}
