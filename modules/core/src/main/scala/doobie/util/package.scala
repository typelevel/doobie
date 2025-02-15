// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie

import scala.reflect.ClassTag

/** Collection of modules for typeclasses and other helpful bits. */
package object util {
  val unlabeled: String = "unlabeled"

  private[util] def void(a: Any*): Unit = {
    val _ = a
  }

  private[doobie] def arraySequence[A: ClassTag](arr: Array[Option[A]]): Option[Array[A]] = {
    val result = new Array[A](arr.length)
    var i = 0
    while (i < arr.length) {
      arr(i) match {
        case None => return None
        case Some(value) =>
          result(i) = value
          i += 1
      }
    }
    Some(result)
  }

}
