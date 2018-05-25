// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

/** Collection of modules for typeclasses and other helpful bits. */
package object util {

  private[util] def void(a: Any*): Unit =
    (a, ())._2

}
