// package doobie

// import scalaz._
// import Scalaz._
// import world.StatementWorld
// import world.StatementWorld._
// import java.sql._

// sealed abstract class Primitive[A] private (sqlType: Int) extends Param[A] {
//   def setNull: Action[Unit] =
//     mkSetNull(sqlType)
// }

// object Primitive {
//   def apply[A](implicit A: Primitive[A]): Primitive[A] = A

//   private def mkPrimitive[A](t: Int, s: A => Action[Unit]): Primitive[A] =
//     new Primitive[A](t) {
//       val set = s
//     }

//   // Primitives
//   implicit val pInt: Primitive[Int] = mkPrimitive[Int](Types.INTEGER, mkSet(_.setInt))
//   implicit val pString: Primitive[String] = mkPrimitive[String](Types.VARCHAR, mkSet(_.setString))

// }


