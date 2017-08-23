package doobie.syntax

trait AllSyntax
  extends ToMonadErrorOps
     with ToFoldableOps
     with ToConnectionIOOps
     with ToStreamOps
     with ToSqlInterpolator

object all extends AllSyntax
