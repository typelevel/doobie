// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.otel4s

/** Controls query text/parameter capture.
  */
sealed trait QueryCaptureConfig {

  /** Query text capture policy for `db.query.text`. */
  def queryTextPolicy: QueryCaptureConfig.QueryTextPolicy

  /** Parameter capture policy for `db.query.parameter.*` attributes.
    */
  def queryParametersPolicy: QueryCaptureConfig.QueryParametersPolicy
}

object QueryCaptureConfig {

  sealed trait QueryTextPolicy

  object QueryTextPolicy {

    /** Never capture query text. */
    case object None extends QueryTextPolicy

    /** Capture query text only when the query has bound parameters. */
    case object ParameterizedOnly extends QueryTextPolicy

    /** Always capture query text. */
    case object Always extends QueryTextPolicy
  }

  sealed trait QueryParametersPolicy

  object QueryParametersPolicy {

    /** Never capture query parameters. */
    case object None extends QueryParametersPolicy

    /** Capture parameters only for non-batch operations. */
    case object NonBatchOnly extends QueryParametersPolicy

    /** Capture parameters for both non-batch and batch operations. */
    case object All extends QueryParametersPolicy
  }

  /** Capture parameterized query text only, with parameters disabled. */
  val recommended: QueryCaptureConfig =
    QueryCaptureConfig(
      queryTextPolicy = QueryTextPolicy.ParameterizedOnly,
      queryParametersPolicy = QueryParametersPolicy.None
    )

  /** Both query text and parameters capture disabled. */
  val disabled: QueryCaptureConfig =
    QueryCaptureConfig(
      queryTextPolicy = QueryTextPolicy.None,
      queryParametersPolicy = QueryParametersPolicy.None
    )

  /** Builds query capture settings.
    *
    * @param queryTextPolicy
    *   policy controlling whether query text is captured in `db.query.text`
    *
    * @param queryParametersPolicy
    *   policy controlling whether parameters are captured for non-batch and/or batch operations
    */
  def apply(
      queryTextPolicy: QueryTextPolicy,
      queryParametersPolicy: QueryParametersPolicy
  ): QueryCaptureConfig =
    QueryCaptureConfigImpl(queryTextPolicy, queryParametersPolicy)

  private final case class QueryCaptureConfigImpl(
      queryTextPolicy: QueryTextPolicy,
      queryParametersPolicy: QueryParametersPolicy
  ) extends QueryCaptureConfig

}
