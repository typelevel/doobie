package doobie.syntax

trait AllSyntax
  extends ToMonadErrorOps
     with ToFoldableOps
     with ToConnectionIOOps
     with ToStreamOps
     with ToSqlInterpolator
     with ToAlignSyntax

object all extends AllSyntax
