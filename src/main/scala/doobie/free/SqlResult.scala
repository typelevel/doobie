package doobie.free


sealed abstract class SqlResult[+A]
sealed abstract class SqlFailure extends SqlResult[Nothing]

object SqlResult {

  final case class Success[A](a: A) extends SqlResult[A]
  final case class Failure(t: Throwable) extends SqlFailure
  
  abstract class Disappointment(msg: String) extends SqlFailure

  // TODO: this is a monad (at least)
}
