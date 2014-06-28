package doobie.hi

import doobie.enum.holdability._
import doobie.free.{ connection => C }
import doobie.free.{ preparedstatement => PS }
import doobie.free.{ resultset => RS }

object connection {
  
  type ConnectionIO[A] = C.ConnectionIO[A]

  implicit val MonadConnectionIO = C.MonadConnectionIO
  implicit val CatchableConnectionIO = C.CatchableConnectionIO

  def getHoldability: ConnectionIO[Holdability] =
    C.getHoldability.map(Holdability.unsafeFromInt)

  def prepareStatementK[A](sql: String)(k: PS.PreparedStatementIO[A]): ConnectionIO[A] =
    C.prepareStatement(sql).flatMap(s => C.liftPreparedStatement(s, k /* .ensuring(PS.close) */))

  // prepareStatementK[Int](null)(null).liftK[Task]

}