package doobie.syntax

import doobie.util.atom._
import doobie.util.composite._
import doobie.syntax.process._

import doobie.hi._

import scalaz.Monad
import scalaz.syntax.monad._
import scalaz.stream.Process

object string {

  implicit class SqlInterpolator(val sc: StringContext) {

    trait ProcessSource {
      
      def executeUpdate: ConnectionIO[Int] 

      def process[O: Composite]: Process[ConnectionIO, O]

      def list[O: Composite]: ConnectionIO[List[O]] =
        process[O].list

      def vector[O: Composite]: ConnectionIO[Vector[O]] =
        process[O].vector

      def sink[O: Composite](f: O => ConnectionIO[Unit]): ConnectionIO[Unit] =
        process[O].sink(f)

    }

    class Source[A: Composite](a: A) extends ProcessSource {

      def executeUpdate: ConnectionIO[Int] =
        connection.prepareStatement(sc.parts.mkString("?"))(preparedstatement.set(a) >> preparedstatement.executeUpdate)

      def process[O: Composite]: Process[ConnectionIO, O] =
        connection.process[O](sc.parts.mkString("?"), preparedstatement.set(a))

    }

    class Source0 extends ProcessSource { 

      def executeUpdate: ConnectionIO[Int] =
        connection.prepareStatement(sc.parts.mkString("?"))(preparedstatement.executeUpdate)

      def process[O: Composite]: Process[ConnectionIO, O] =
        connection.process[O](sc.parts.mkString("?"), Monad[PreparedStatementIO].point(()))

    }

    def sql(): ProcessSource = new Source0
    def sql[A: Atom](a: A): ProcessSource = new Source(a)
    def sql[A: Atom, B: Atom](a: A, b: B): ProcessSource = new Source((a, b))
    def sql[A: Atom, B: Atom, C: Atom](a: A, b: B, c: C): ProcessSource = new Source((a, b, c))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom](a: A, b: B, c: C, d: D): ProcessSource = new Source((a, b, c, d))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom](a: A, b: B, c: C, d: D, e: E): ProcessSource = new Source((a, b, c, d, e))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom](a: A, b: B, c: C, d: D, e: E, f: F): ProcessSource = new Source((a, b, c, d, e, f))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G): ProcessSource = new Source((a, b, c, d, e, f, g))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H): ProcessSource = new Source((a, b, c, d, e, f, g, h))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I): ProcessSource = new Source((a, b, c, d, e, f, g, h, i))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom, U: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u))
    def sql[A: Atom, B: Atom, C: Atom, D: Atom, E: Atom, F: Atom, G: Atom, H: Atom, I: Atom, J: Atom, K: Atom, L: Atom, M: Atom, N: Atom, O: Atom, P: Atom, Q: Atom, R: Atom, S: Atom, T: Atom, U: Atom, V: Atom](a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, o: O, p: P, q: Q, r: R, s: S, t: T, u: U, v: V): ProcessSource = new Source((a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v))

  }

}
