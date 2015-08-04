package doobie.contrib.informix.hi

import com.informix.jdbc.{ IfxConnection, IfxSmartBlob }

import doobie.contrib.informix.free.{ ifxconnection => IFXC }
import doobie.contrib.informix.free.ifxconnection.IfxConnectionIO
import doobie.contrib.informix.free.ifxsmartblob.IfxSmartBlobIO

import doobie.imports._

object ifxconnection {

  def getIfxSmartBlob[A](k: IfxSmartBlobIO[A]): IfxConnectionIO[A] =
    IFXC.getIfxSmartBlob.flatMap(s => IFXC.liftIfxSmartBlob(s, k)) // N.B. no need to close()

}
