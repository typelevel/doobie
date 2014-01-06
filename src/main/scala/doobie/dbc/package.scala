package doobie

import scalaz._
import scalaz.syntax.monad._
import scalaz.syntax.effect.monadCatchIO._
import scalaz.effect._
import scalaz.effect.IO._
import scalaz.effect.kleisliEffect._
import language._

package object dbc {

  type Savepoint = java.sql.Savepoint

  type Connection[+A] = connection.Connection[A]  
  type Statement[+A] = statement.Statement[A]
  type DatabaseMetaData[+A] = databasemetadata.DatabaseMetaData[A]
  type CallableStatement[+A] = callablestatement.CallableStatement[A]
  type PreparedStatement[+A] = preparedstatement.PreparedStatement[A]
  type ResultSet[+A] = resultset.ResultSet[A]

  def log[M[+_]: MonadCatchIO, A](s: => String, ma: M[A]): M[A] =
    for {
      _ <- IO.putStr(Console.BLUE + s + Console.RESET).liftIO[M]
      t <- IO(System.nanoTime).liftIO[M]
      a <- ma.except(t => (IO.putStrLn(s" -> ${Console.RED + Console.BLINK}** $t.getMessage${Console.RESET}") >> IO[A](throw t)).liftIO[M])
      t <- IO(System.nanoTime - t).liftIO[M]
      _ <- IO.putStrLn(s" -> ${Console.GREEN}$a${Console.RESET} (${t/1000} µs)").liftIO[M]
    } yield a

  type LogElement = String // for now

}

