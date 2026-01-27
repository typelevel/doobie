// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.util

import doobie.util.log.LoggingInfo

object trace {

  sealed trait TraceEvent extends Product with Serializable

  object TraceEvent {

    sealed trait ExecutePreparedStatement extends TraceEvent {
      def loggingInfo: LoggingInfo
    }

    private[doobie] def executePreparedStatement(query: LoggingInfo): ExecutePreparedStatement =
      ExecutePreparedStatementImpl(query)

    private final case class ExecutePreparedStatementImpl(loggingInfo: LoggingInfo) extends ExecutePreparedStatement
  }

}
