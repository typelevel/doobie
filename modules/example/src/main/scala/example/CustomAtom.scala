// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package example

 import doobie._, doobie.implicits._

 import java.sql.Date

 object CustomMeta {

   final case class PosixTime(time: Long)

   // Create our base Meta by invariant mapping an existing one.
   implicit val LongPosixTimeScalaType: Meta[PosixTime] =
     Meta[Date].timap(d => PosixTime(d.getTime))(t => new Date(t.time))

   // What we just defined
   val m = Meta[PosixTime]

   // Free derived composites containing atomic types
   val c1 = Composite[(PosixTime, Int, String)]
   val c2 = Composite[(Option[PosixTime], Int, String)]

   // You can now use PosixTime as a column or parameter type (both demonstrated here)
   def query(lpt: PosixTime): Query0[(String, PosixTime)] =
     sql"SELECT NAME, DATE FROM FOO WHERE DATE > $lpt".query[(String, PosixTime)]

 }
