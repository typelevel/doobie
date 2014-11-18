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

/** String interpolator for SQL literals. */
object string {

  implicit class SqlInterpolator(val sc: StringContext) {

    val stackFrame = {
      import Predef._
      Thread.currentThread.getStackTrace.lift(3)
    }

    val rawSql = sc.parts.mkString("?")

    trait Builder {
      def query[A: Composite]: Query0[A]
      def update: Update0
    }

    class Source[A: Composite](a: A) extends Builder {
      def query[O: Composite] = Query[A, O](rawSql, stackFrame).toQuery0(a)
      def update = Update[A](rawSql, stackFrame).toUpdate0(a)
    }

    class Source0 extends Builder { 
      def query[O: Composite] = Query0(rawSql, stackFrame)
      def update = Update0(rawSql, stackFrame)
    }

    def sql(): Builder = new Source0
    def sql[A: Atom](a: A): Builder = new Source(a)
    def sql[A: Atom, B: Atom](a: A, b: B): Builder = new Source((a, b))
    def sql[A: Atom, B: Atom, C: Atom](a: A, b: B, c: C): Builder = new Source((a, b, c))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom](a: A, b: B, c: C, d: D): Builder = new Source((a, b, c, d))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom](a: A, b: B, c: C, d: D, e: E): Builder = new Source((a, b, c, d, e))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom](a: A, b: B, c: C, d: D, e: E, f: F): Builder = new Source((a, b, c, d, e, f))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G): Builder = new Source((a, b, c, d, e, f, g))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H): Builder = new Source((a, b, c, d, e, f, g, h))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I): Builder = new Source((a, b, c, d, e, f, g, h, i))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J): Builder = new Source((a, b, c, d, e, f, g, h, i, j))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom, U: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom, U: Atom, V: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U, v: V): Builder = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v))

  }

}
