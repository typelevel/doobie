package doobie.example.todo

import doobie._
import doobie.hi._
import java.sql.SQLException

import scalaz._, Scalaz._
import scalaz.effect._, stateTEffect._, IO._
import syntax.effect.monadCatchIO._

object Repl {

  ////// ENTRY POINT

  def run(db: Database): IO[Unit] =
    loop.eval(ReplState.initial(db))

  ////// REPL STATE

  case class ReplState(topic: Option[String], db: Database)
  object ReplState {
    def initial(d: Database): ReplState =
      ReplState(None, d)
  }

  type Repl[+A] = StateT[IO, ReplState, A]

  def topic: Repl[Option[String]] =
    get[ReplState].map(_.topic).lift[IO]

  def db: Repl[Database] =
    get[ReplState].map(_.db).lift[IO]

  ////// MAIN LOOP

  def loop: Repl[Unit] =
    prompt >> readLn.map(Option(_)).liftIO[Repl] >>= interp >>= (_ whenM loop)

  def prompt: Repl[Unit] =
    topic >>= (t => putStr(s"${t.getOrElse("default")}> ").liftIO[Repl])

  def interp(s: Option[String]): Repl[Boolean] = 
    s.map(interp0).getOrElse(kick)

  def interp0(s: String): Repl[Boolean] =
    s.split("\\s+").toList match {
      case List("topics") => topics
      case List("mktopic", name) => mktopic(name)
      case List("quit")   => quit
      case List("help")   => help
      case List("")       => true.point[Repl]
      case _              => wat
    }

  ////// COMBINATORS

  type Command = Repl[Boolean]

  def dbCommand(cmd: String)(a: Connection[Boolean]): Command =
    db >>= { d =>

      // todo: catchSomeLeft[Exception], set or unset last exception, always set last log

      val action: IO[Boolean] =
        for {
          l <- util.TreeLogger.newLogger(LogElement(cmd))
          a <- d.run(a, l).except { 
                case e: SQLException =>  
                  // SQLState codes here:
                  for {
                    _ <- IO.putStrLn(s"SQLState: ${e.getSQLState}")
                    _ <- IO.putStrLn(s"${Console.RED}${e.getMessage}${Console.RESET}")
                } yield true
                case t: Throwable => throw t
              }
        } yield a

      action.liftIO[Repl]

    }

  ////// COMMANDS

  def wat: Command =
    putStrLn("wat? try 'help' for help").liftIO[Repl] >| true

  def help: Command =
    putStrLn("""
    |  help          print this summary
    |  topics        list all topics
    |  quit          exit the app (also ^D)
    """.trim.stripMargin).liftIO[Repl] >| true

  def quit: Command =
    false.point[Repl]

  def kick: Command =
    putStrLn("^D").liftIO[Repl] >> quit

  def topics: Command =
    dbCommand("topics") {
      DAO.topics(t => IO.putStrLn(t.toString)) >| true
    } 

  def mktopic(name: String): Command =
    dbCommand(s"mktopic($name)") {

      val action: Connection[Boolean] =
        for { 
          t <- DAO.insertTopic(name) >> DAO.lastIdentity >>= DAO.selectTopic
          _ <- IO.putStrLn(t.toString).liftIO[Connection]
        } yield true

      // ftp://ftp.software.ibm.com/ps/products/db2/info/vr6/htm/db2m0/db2state.htm#HDRSTTMSG
      action.catchSqlState("23505") {
        IO.putStrLn("[error] A topic of this name already exists.").liftIO[Connection] >| true
      }

    }

}




