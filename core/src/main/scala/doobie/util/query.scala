package doobie.util

import scala.collection.generic.CanBuildFrom

import doobie.imports._
import doobie.util.analysis.Analysis

import scalaz.{ MonadPlus, Profunctor, Contravariant, Functor }
import scalaz.stream.Process
import scalaz.syntax.monad._

/** Module defining queries parameterized by input and output types. */
object query {

  /** 
   * A `Query` parameterized by some input type `A` yielding values of type `B`. We define here the
   * core operations that are needed. Additional operations are provided on `Query0` which is the 
   * risidual query after applying an `A`. This is the type constructed by the `sql` interpreter.
   */
  trait Query[A, B] { outer =>

    // jiggery pokery to support CBF; we're doing the coyoneda trick on B to to avoid a Functor
    // constraint on the `F` parameter in `to`, and it's just easier to do the contravariant coyo 
    // trick on A while we're at it.
    protected type I
    protected type O
    protected val ai: A => I
    protected val ob: O => B
    protected implicit val ic: Composite[I]
    protected implicit val oc: Composite[O]

    def sql: String
    
    def stackFrame: Option[StackTraceElement]

    def analysis: ConnectionIO[Analysis] =
      HC.prepareQueryAnalysis[I, O](sql)

    def process(a: A): Process[ConnectionIO, B] = 
      HC.process[O](sql, HPS.set(ai(a))).map(ob)

    def to[F[_]](a: A)(implicit cbf: CanBuildFrom[Nothing, B, F[B]]): ConnectionIO[F[B]] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeQuery(HRS.buildMap[F,O,B](ob)))

    def accumulate[F[_]: MonadPlus](a: A): ConnectionIO[F[B]] = 
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeQuery(HRS.accumulate[F, O].map(_.map(ob))))

    def unique(a: A): ConnectionIO[B] = 
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeQuery(HRS.getUnique[O])).map(ob)

    def option(a: A): ConnectionIO[Option[B]] = 
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeQuery(HRS.getOption[O])).map(_.map(ob))

    def map[C](f: B => C): Query[A, C] =
      new Query[A, C] {
        type I = outer.I
        type O = outer.O
        val ai = outer.ai
        val ob = outer.ob andThen f
        val ic: Composite[I] = outer.ic 
        val oc: Composite[O] = outer.oc
        def sql = outer.sql
        def stackFrame = outer.stackFrame
      }

    def contramap[C](f: C => A): Query[C, B] =
      new Query[C, B] {
        type I = outer.I
        type O = outer.O
        val ai = outer.ai compose f
        val ob = outer.ob
        val ic: Composite[I] = outer.ic 
        val oc: Composite[O] = outer.oc
        def sql = outer.sql
        def stackFrame = outer.stackFrame
      }

    def toQuery0(a: A): Query0[B] =
      new Query0[B] {
        def sql = outer.sql
        def stackFrame = outer.stackFrame
        def analysis = outer.analysis
        def process = outer.process(a)
        def to[F[_]](implicit cbf: CanBuildFrom[Nothing, B, F[B]]) = outer.to[F](a)
        def accumulate[F[_]: MonadPlus] = outer.accumulate[F](a)  
        def unique = outer.unique(a)
        def option = outer.option(a)
        def map[C](f: B => C): Query0[C] = outer.map(f).toQuery0(a)
      }

  }

  object Query {

    def apply[A, B](sql0: String, stackFrame0: Option[StackTraceElement])(implicit A: Composite[A], B: Composite[B]): Query[A, B] =
      new Query[A, B] {
        type I = A
        type O = B
        val ai: A => I = a => a
        val ob: O => B = o => o
        implicit val ic: Composite[I] = A
        implicit val oc: Composite[O] = B
        val sql = sql0
        val stackFrame = stackFrame0
      }

    implicit val queryProfunctor: Profunctor[Query] =
      new Profunctor[Query] {
        def mapfst[A, B, C](fab: Query[A,B])(f: C => A) = fab contramap f
        def mapsnd[A, B, C](fab: Query[A,B])(f: B => C) = fab map f
      }

    implicit def queryCovariant[A]: Functor[({ type l[b] = Query[A, b]})#l] =
      queryProfunctor.covariantInstance[A]

    implicit def queryContravariant[B]: Contravariant[({ type l[a] = Query[a, B]})#l] =
      queryProfunctor.contravariantInstance[B]

  }

  /** A `Query` applied to its argument. */
  trait Query0[B] { outer =>

    def sql: String  
    def stackFrame: Option[StackTraceElement]
    def process: Process[ConnectionIO, B]
    def analysis: ConnectionIO[Analysis] 
    def to[F[_]](implicit cbf: CanBuildFrom[Nothing, B, F[B]]): ConnectionIO[F[B]] 
    def accumulate[F[_]: MonadPlus]: ConnectionIO[F[B]]  
    def unique: ConnectionIO[B]  
    def option: ConnectionIO[Option[B]]  
    def map[C](f: B => C): Query0[C] 

    // Convenience methods
    def sink(f: B => ConnectionIO[Unit]): ConnectionIO[Unit] = process.sink(f)
    def list: ConnectionIO[List[B]] = to[List]
    def vector: ConnectionIO[Vector[B]] = to[Vector]

  }

  object Query0 {
    implicit val queryFunctor: Functor[Query0] =
      new Functor[Query0] {
        def map[A, B](fa: Query0[A])(f: A => B) = fa map f
      }
  }

}
