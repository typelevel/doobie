package doobie.syntax

import doobie.util.atom._
import doobie.util.composite._
import doobie.util.query._
import doobie.util.update._
import doobie.syntax.process._

import doobie.hi._

import scalaz.Monad
import scalaz.syntax.monad._
import scalaz.stream.Process

object string {

  implicit class SqlInterpolator(val sc: StringContext) {

    val rawSql = sc.parts.mkString("?")

    class Source[A: Composite](a: A) {
      def executeUpdate = Update[A](rawSql).run(a)
      def process[O: Composite] = Query[A, O](rawSql).run(a)
    }

    class Source0 { 
      def executeUpdate = Update0(rawSql).run
      def process[O: Composite] = Query0(rawSql).run
    }

    def sql() = new Source0
    def sql[A: Atom](a: A) = new Source(a)
    def sql[A: Atom, B: Atom](a: A, b: B) = new Source((a, b))
    def sql[A: Atom, B: Atom, C: Atom](a: A, b: B, c: C) = new Source((a, b, c))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom](a: A, b: B, c: C, d: D) = new Source((a, b, c, d))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom](a: A, b: B, c: C, d: D, e: E) = new Source((a, b, c, d, e))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom](a: A, b: B, c: C, d: D, e: E, f: F) = new Source((a, b, c, d, e, f))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G) = new Source((a, b, c, d, e, f, g))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H) = new Source((a, b, c, d, e, f, g, h))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I) = new Source((a, b, c, d, e, f, g, h, i))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J) = new Source((a, b, c, d, e, f, g, h, i, j))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K) = new Source((a, b, c, d, e, f, g, h, i, j, k))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L) = new Source((a, b, c, d, e, f, g, h, i, j, k, l))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom, U: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom, U: Atom, V: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U, v: V) = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v))

  }

}
