// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package org.typelevel.doobie.otel4s

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.typelevel.otel4s.Attributes
import org.typelevel.vault.Key
import org.typelevel.vault.Vault

/** The Vault key used to attach otel4s attributes to a query or update. */
object AttributesMetadata {
  val key: Key[Attributes] = Key.newKey[IO, Attributes].unsafeRunSync()

  /** Add attributes to the existing set. New values win when a key is repeated. */
  def add(metadata: Vault, attributes: Attributes): Vault =
    metadata.insert(key, metadata.lookup(key).getOrElse(Attributes.empty) ++ attributes)
}
