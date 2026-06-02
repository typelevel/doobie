// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

import org.typelevel.doobie.util.log.LoggingInfo

object trace {

  sealed trait TraceEvent {
    def loggingInfo: LoggingInfo
  }

  object TraceEvent {

    private[doobie] def apply(loggingInfo: LoggingInfo): TraceEvent =
      TraceEventImpl(loggingInfo)

    private final case class TraceEventImpl(loggingInfo: LoggingInfo) extends TraceEvent

  }

}
