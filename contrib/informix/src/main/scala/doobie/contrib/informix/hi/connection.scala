package doobie.contrib.informix.hi

import com.informix.jdbc.{ IfxSqliConnect, IfxSmartBlob }

import doobie.contrib.informix.free.ifxconnection.IfxConnectionIO
import doobie.contrib.informix.free.ifxsmartblob.IfxSmartBlobIO
import doobie.contrib.informix.hi.{ ifxconnection => HIFXC }

import doobie.imports._

import scalaz.syntax.functor._

/** Module of safe `IfxConnectionIO` operations lifted into `ConnectionIO`. */
object connection {

  def ifxGetConnection[A](k: IfxConnectionIO[A]): ConnectionIO[A] =
    FC.unwrap(classOf[IfxSqliConnect]) >>= k.transK[ConnectionIO]

  def ifxGetIfxSmartBlob[A](k: IfxSmartBlobIO[A]): ConnectionIO[A] =
    ifxGetConnection(HIFXC.getIfxSmartBlob(k))

}
