import org.specs2.mutable._

import java.io.File

import scalaz.concurrent.Task
import scalaz._, Scalaz._, scalaz.\&/._

import doobie.enum.nullability._
import doobie.hi._
import doobie.hi.connection.ProcessConnectionIOOps
import doobie.std.task._
import doobie.std.string._
import doobie.std.int._
import doobie.std.double._
import doobie.syntax.process._
import doobie.syntax.string._
import doobie.util.transactor._
import doobie.example.HiUsage2DAO._
import doobie.util.indexed.Indexed.Meta
import doobie.util.prepared._

abstract class PreparedSpec(transactor: Transactor[Task], setup: ConnectionIO[Unit], ps: (Prepared, String)*) extends Specification {
  ps.foreach { case (p, name) => 
    val a = transactor.transact(setup *> p.check).run
    (s"""Query or Update "$name":\n""" + a.dump + "\n") should {
        a.columns.zipWithIndex.foreach { case (c, n0) =>
          val n = n0 + 1
          s"Have proper null handling for column $n" in {
            c match {
              case Both(Meta(ta, Nullable), Meta(tb, NoNulls)) => failure(s"Column $n is nullable, but is not mapped to an Option type.")
              case Both(Meta(ta, NoNulls), Meta(tb, Nullable)) => failure(s"Column $n will never be null, but is mapped to an Option type.")
              case _ => skipped("-- n/a")
            }
          }
        }
      }
    }

}

object HiUsage2DAOSpec extends PreparedSpec(
  DriverManagerTransactor[Task]("org.h2.Driver", "jdbc:h2:mem:test", "sa", ""),
  loadDatabase(new File("world.sql")).run.void,
  // (speakerQuery("ignored", 0), "Speaker Query"),
  (speakerQuery("ignored", 0), "Speaker Query")
)


