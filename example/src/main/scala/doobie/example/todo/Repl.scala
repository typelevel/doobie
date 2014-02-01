package doobie.example.todo

import doobie._
import doobie.hi._

import scalaz._, Scalaz._
import scalaz.effect._, stateTEffect._, IO._

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
      case List("quit")   => quit
      case List("help")   => help
      case List("")       => true.point[Repl]
      case _              => wat
    }

  ////// COMBINATORS

  def dbCommand[A](cmd: String)(a: Connection[A]): Repl[A] =
    for {
      l <- util.TreeLogger.newLogger(LogElement(cmd)).liftIO[Repl]
      a <- db >>= (_.run(a, l).liftIO[Repl])
    } yield a

  ////// COMMANDS

  def wat: Repl[Boolean] =
    putStrLn("wat? try 'help' for help").liftIO[Repl] >| true

  def help: Repl[Boolean] =
    putStrLn("""
    |  help          print this summary
    |  topics        list all topics
    |  quit          exit the app (also ^D)
    """.trim.stripMargin).liftIO[Repl] >| true

  def quit: Repl[Boolean] =
    false.point[Repl]

  def kick: Repl[Boolean] =
    putStrLn("^D").liftIO[Repl] >> quit

  def topics: Repl[Boolean] =
    dbCommand("topics") {
      DAO.topics(t => IO.putStrLn(t.toString)) >| true
    }

}

