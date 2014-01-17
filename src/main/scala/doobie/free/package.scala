package doobie

import scalaz.{ Tree, TreeLoc, Free }
import shapeless._

package object free {

  // Free types
  type ResultSetOp[+A] = Free[ResultSetOpF, A]

  // Coproduct type for operations as they appear in the log. It's ok to crush the type parameter 
  // here because it's only used by the continuation (which has no meaning here). We can't use an
  // existential; coproduct doesn't like them.
  type GenOp = ResultSetOpF[Any] :+: CNil

  // Remaining aliases for logging
  type GenResult = SqlResult[_]
  type Nano = Long
  type Log = Tree[LogEntry]
  type LogZipper = TreeLoc[LogEntry]

}