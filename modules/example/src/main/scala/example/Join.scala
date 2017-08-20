package doobie.example

import doobie.imports._
import scalaz._, Scalaz._

object Join {

  case class Country(code: String, name: String)
  case class City(id: Int, name: String)

  // Old style required examining joined columns individually
  def countriesAndCities1(filter: String): Query0[(Country, Option[City])] =
    sql"""
      SELECT k.code, k.name, c.id, c.name
      FROM country k
      LEFT OUTER JOIN city c
        ON k.code = c.countrycode AND c.name like $filter
    """.query[(Country, Option[Int], Option[String])]
       .map { case (k, a, b) => (k, (a |@| b)(City)) }

  // New style (better)
  def countriesAndCities2(filter: String): Query0[(Country, Option[City])] =
    sql"""
      SELECT k.code, k.name, c.id, c.name
      FROM country k
      LEFT OUTER JOIN city c
        ON k.code = c.countrycode AND c.name like $filter
    """.query

}
