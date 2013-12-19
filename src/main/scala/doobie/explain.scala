package doobie

import scalaz._
import Scalaz._
import scalaz.effect.IO

object explain {

  object database {

    import world.database.{Log, Event}

    def showLog(log: Log): IO[Unit] =
      log.traverse_(showEvent)

    def showEvent(e: Event): IO[Unit] =
      e match {
        case Event.ConnectionLog(log) => connection.showLog(log)
        case e => IO.putStrLn(e.toString)
      }

  }

  object connection {

    import world.connection.{Log, Event}

    def putStrLn(s: String): IO[Unit] =
      IO.putStrLn("  " + s)

    def showLog(log: Log): IO[Unit] =
      log.traverse_(showEvent)

    def showEvent(e: Event): IO[Unit] =
      e match {
        case Event.StatementLog(log) => statement.showLog(log)
        case e => putStrLn(e.toString)
      }

  }

  object statement {

    import world.statement.{Log, Event}

    def putStrLn(s: String): IO[Unit] =
      IO.putStrLn("    " + s)

    def showLog(log: Log): IO[Unit] =
      log.traverse_(showEvent)

    def showEvent(e: Event): IO[Unit] =
      e match {
        case e => putStrLn(e.toString)
      }

  }

}