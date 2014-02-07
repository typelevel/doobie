package doobie.example.todo

import doobie._
import doobie.hi._
import java.sql.SQLException

import scalaz._, Scalaz._
import scalaz.effect._, stateTEffect._, IO._
import syntax.effect.monadCatchIO._

object Repl {
  import Model._

  ////// ENTRY POINT

  def run(db: Database): IO[Unit] =
    loop.eval(ReplState.initial(db))

  ////// REPL STATE

  case class ReplState(
    db:    Database, 
    topic: Option[Topic], 
    log:   Option[Tree[util.TreeLogger.Node[LogElement]]], 
    ex:    Option[Exception])

  object ReplState {
    def initial(d: Database): ReplState =
      ReplState(d, None, None, None)
  }

  type Repl[+A] = StateT[IO, ReplState, A]

  def gets[A](f: ReplState => A): Repl[A] =
    get[ReplState].map(f).lift[IO]

  def mod(f: ReplState => ReplState): Repl[Unit] =
    modify(f).lift[IO]

  ////// MAIN LOOP

  def loop: Repl[Unit] =
    prompt >> readLn.map(Option(_)).liftIO[Repl] >>= interp >>= (_ whenM loop)

  def prompt: Repl[Unit] =
    gets(_.topic) >>= (t => putStr(s"todo:${t.map(_.name).getOrElse("/")}$$ ").liftIO[Repl])

  def interp(s: Option[String]): Repl[Boolean] = 
    s.map(interp0).getOrElse(kick)

  def interp0(s: String): Repl[Boolean] =    
    s.trim.split("\\s+").toList match {
      case List("ls")          => topics         >| true
      case List("mkdir", name) => mktopic(name)  >| true
      case List("cd", name)    => cd(name)       >| true
      case List("quit")        => quit
      case List("help")        => help           >| true
      case List("last")        => last           >| true
      case List("")            => ().point[Repl] >| true
      case _                   => wat            >| true
    }
  
  ////// COMMANDS

  def wat: Repl[Unit] =
    err("wat? try 'help' for help").liftIO[Repl]

  def help: Repl[Unit] =
    putStrLn("""
    |  help            print this summary
    |  last            show log from last command
    |  ls              list all topics
    |  mkdir <name>    create a new topic
    |  cd <name>       set the current topic
    |  quit            exit the app (also ^D)
    """.trim.stripMargin).liftIO[Repl]

  def quit: Repl[Boolean] =
    false.point[Repl]

  def kick: Repl[Boolean] =
    putStrLn("^D").liftIO[Repl] >> quit

  def last: Repl[Unit] = // TODO: last exception
    gets(_.log) >>= {
      case None    => putStrLn("No log available.").liftIO[Repl]
      case Some(t) => util.TreeLogger.drawTree(t).liftIO[Repl]  
    }

  def topics: Repl[Exception \/ Unit] =
    dbCommand("ls") {
      DAO.topics(t => IO.putStrLn(t.name))
    }

  def mktopic(name: String): Repl[Exception \/ Option[Topic]] =
    dbCommand(s"mkdir $name") {
      for {
        t <- Topic.create(name)
        _ <- t.isDefined.unlessM(err(s"mkdir: $name: topic exists").liftIO[Connection])
      } yield t
    } 

  def cd(name: String): Repl[Unit] =
    dbCommand(s"cd $name") {
      DAO.selectTopicByName(name)
    } >>= {
      case \/-(None)        => err(s"cd: $name: no such topic").liftIO[Repl]
      case \/-(s @ Some(t)) => mod(_.copy(topic = s)) 
      case _                => ().point[Repl]
    } 

  ////// MISC

  def err(s: String): IO[Unit] =
    putStrLn(s"${Console.RED}[err]${Console.RESET} $s")

  def dbCommand[A](cmd: String)(a: Connection[A]): Repl[Exception \/ A] =
    for {
      d <- gets(_.db)
      l <- util.TreeLogger.newLogger(LogElement(cmd)).liftIO[Repl]
      e <- d.run(a, l).catchSomeLeft(justExceptions).liftIO[Repl]
      _ <- e.fold(e => err("An error occurred. Use `last` for details."), _ => IO.ioUnit).liftIO[Repl]
      _ <- mod(_.copy(ex = e.swap.toOption)) 
      _ <- l.tree.liftIO[Repl] >>= (t => mod(_.copy(log = Some(t))))
    } yield e

  // N.B. this will be unnecessary in 7.1
  def justExceptions(t: Throwable): Option[Exception] =
    PartialFunction[Throwable, Exception] {
      case e: Exception => e
    } lift(t)

}




