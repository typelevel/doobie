package doobie.hi
package syntax

import java.sql
import scalaz._
import Scalaz._
import scalaz.effect.{IO, MonadIO}
import scalaz.syntax.effect.monadCatchIO._
import scalaz.stream._
import Process._

class SqlInterpolator(val sc: StringContext) {
  import connection.prepareStatement

  class Source[A: Comp](a: A) {

    def go[B](b: PreparedStatement[B]): Connection[B] =
      prepareStatement(sc.parts.mkString("?"))(preparedstatement.set(1, a) >> b)

    def executeQuery[B](b: ResultSet[B]): Connection[B] =
      go(preparedstatement.executeQuery(b))
  
    def execute: Connection[Boolean] =
      go(preparedstatement.execute)

    def executeUpdate: Connection[Int] =
      go(preparedstatement.executeUpdate)

    def process[O: Comp]: Process[Connection, O] =
      connection.process[O](sc.parts.mkString("?"), preparedstatement.set1(a))

  }

  class Source0 extends Source[Int](1) { // TODO: fix this

    override def go[B](b: PreparedStatement[B]): Connection[B] =
      prepareStatement(sc.parts.mkString("?"))(b)

    override def process[O: Comp]: Process[Connection, O] =
      connection.process[O](sc.parts.mkString("?"),().point[preparedstatement.Action])

  }

  def sql() = new Source0
  def sql[A: Prim](a: A) = new Source(a)
  def sql[A: Prim, B: Prim](a: A, b: B) = new Source((a, b))
  def sql[A: Prim, B: Prim, C: Prim](a: A, b: B, c: C) = new Source((a, b, c))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim](a: A, b: B, c: C, d: D) = new Source((a, b, c, d))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim](a: A, b: B, c: C, d: D, e: E) = new Source((a, b, c, d, e))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim](a: A, b: B, c: C, d: D, e: E, f: F) = new Source((a, b, c, d, e, f))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G) = new Source((a, b, c, d, e, f, g))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H) = new Source((a, b, c, d, e, f, g, h))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I) = new Source((a, b, c, d, e, f, g, h, i))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) = new Source((a, b, c, d, e, f, g, h, i, j))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K) = new Source((a, b, c, d, e, f, g, h, i, j, k))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L) = new Source((a, b, c, d, e, f, g, h, i, j, k, l))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim, O: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim, O: Prim, P: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim, O: Prim, P: Prim, Q: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim, O: Prim, P: Prim, Q: Prim, R: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim, O: Prim, P: Prim, Q: Prim, R: Prim, S: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim, O: Prim, P: Prim, Q: Prim, R: Prim, S: Prim, T: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim, O: Prim, P: Prim, Q: Prim, R: Prim, S: Prim, T: Prim, U: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u))
  def sql[A: Prim, B: Prim, C: Prim, D: Prim, E: Prim, F: Prim, G: Prim, H: Prim, I: Prim, J: Prim, K: Prim, L: Prim, M: Prim, N: Prim, O: Prim, P: Prim, Q: Prim, R: Prim, S: Prim, T: Prim, U: Prim, V: Prim](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U, v: V) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v))

  // TODO: remove prior to 0.1
  // to generate the above:
  // def tparams(ls: List[Char]): String = ls.map(c => s"$c: Prim").mkString(", ")
  // def params(ls: List[Char]): String = ls.map(c => s"${c.toLower}: $c").mkString(", ")
  // def args(ls: List[Char]): String = ls.map(_.toLower).mkString(", ")
  // ('A' to 'V').toList.inits.toList.reverse.map { ls => 
  //   s"def sql[${tparams(ls)}](${params(ls)}) = new Source((${args(ls)}))"
  //   }.foreach(println)
  // }

}