// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

/** Controls query text/parameter capture.
  */
sealed trait QueryCaptureConfig {

  /** When true, the SQL text is attached as `db.query.text`.
    */
  def captureQueryStatementText: Boolean

  /** Parameter capture policy for `db.query.parameter.*` attributes.
    */
  def captureQueryStatementParameters: QueryCaptureConfig.QueryParametersPolicy
}

object QueryCaptureConfig {

  /** `db.query.text` enabled and parameters disabled. */
  val recommended: QueryCaptureConfig =
    QueryCaptureConfig(
      captureQueryStatementText = true,
      captureQueryStatementParameters = QueryParametersPolicy.None
    )

  /** Both query text and parameters capture disabled. */
  val disabled: QueryCaptureConfig =
    QueryCaptureConfig(
      captureQueryStatementText = false,
      captureQueryStatementParameters = QueryParametersPolicy.None
    )

  /** Builds query capture settings.
    *
    * @param captureQueryStatementText
    *   when true, attach `db.query.text` with the SQL text
    *
    * @param captureQueryStatementParameters
    *   policy controlling whether parameters are captured for non-batch and/or batch operations
    */
  def apply(
      captureQueryStatementText: Boolean,
      captureQueryStatementParameters: QueryParametersPolicy
  ): QueryCaptureConfig =
    QueryCaptureConfigImpl(captureQueryStatementText, captureQueryStatementParameters)

  private final case class QueryCaptureConfigImpl(
      captureQueryStatementText: Boolean,
      captureQueryStatementParameters: QueryParametersPolicy
  ) extends QueryCaptureConfig

  sealed trait QueryParametersPolicy

  object QueryParametersPolicy {

    /** Never capture query parameters. */
    case object None extends QueryParametersPolicy

    /** Capture parameters only for non-batch operations. */
    case object NonBatchOnly extends QueryParametersPolicy

    /** Capture parameters for both non-batch and batch operations. */
    case object All extends QueryParametersPolicy
  }
}
