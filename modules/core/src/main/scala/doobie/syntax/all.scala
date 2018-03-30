// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.syntax

trait AllSyntax
  extends ToMonadErrorOps
     with ToFoldableOps
     with ToConnectionIOOps
     with ToStreamOps
     with ToSqlInterpolator
     with ToAlignSyntax

object all extends AllSyntax
