package doobie.free

import scalaz.{ State => _, _ }, Scalaz._
import scalaz.effect._, IO._
import resultset._

object Test extends App {

  val action = for {
    _ <- liftIO(IO("a"))
    _ <- liftIO(putStrLn("b"))
    _ <- 123.point[ResultSetOp]
    a <- log("foo", 
      for {
        _ <- liftIO(putStrLn("c"))
        _ <- liftIO(putStrLn("d"))
        // b <- ensuring(getString(1), liftIO(IO("sequal")))
      } yield sys.error("huh"))
  } yield a

  val state = State(Tree[LogEntry](LogEntry.Root("free test")).loc, null)

  val (a, State(zip, rs)) = run(log("action!", action), state).unsafePerformIO
  println(s"answer is $a")
  println()
  println("log:\n" + zip.tree.draw(Show.showA).grouped(2).map(_.head).mkString("\n"))
  println()

}