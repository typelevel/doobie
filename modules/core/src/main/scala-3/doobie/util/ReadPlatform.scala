// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.util

trait ReadPlatform:

  /** Brings an implicit into scope which behaves like [[instance]], but also derives Read instances for case classes */
  trait Auto:
    implicit final inline def autoDerivedRead[A]: Read[A] =
      ${ ReadMacros.instance[A](autoDerivation = true) }

  /** Read instance for any type, constructed by a macro. Being fully generic, the compiler prefers any more specific
    * Read instance in scope over this one. Otherwise the macro looks for, in order:
    *
    *   - For `Option[A]`: a Read instance for `A`, made nullable
    *   - A Get instance, for reading a single column
    *   - For tuples: Read instances for each element
    *   - For case classes, when automatic derivation is enabled: Read instances for each field
    *
    * Read instances for elements and fields are resolved by the same rules.
    */
  inline given instance[A]: Read[A] =
    ${ ReadMacros.instance[A](autoDerivation = false) }

  /** Derive a Read instance for a case class or tuple, using the Read instances for each field */
  inline def derived[A <: Product]: Read[A] =
    ${ ReadMacros.derived[A] }

  def fromGet[A](using get: Get[A]): Read[A] = new Read.Single(get)
