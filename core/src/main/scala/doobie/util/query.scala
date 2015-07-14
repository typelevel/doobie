package doobie.util

import scala.collection.generic.CanBuildFrom

import doobie.imports._
import doobie.util.analysis.Analysis

import scalaz.{ MonadPlus, Profunctor, Contravariant, Functor }
import scalaz.stream.Process
import scalaz.syntax.monad._

/** Module defining queries parameterized by input and output types. */
object query {

  /** Mixin trait for queries with diagnostic information. */
  trait QueryDiagnostics {
    def sql: String
    def stackFrame: Option[StackTraceElement]
    def analysis: ConnectionIO[Analysis]
  }

  trait Query[A, B] extends QueryDiagnostics { outer =>

    // jiggery pokery to support CBF
    protected type I
    protected type O
    protected val ai: A => I
    protected val ob: O => B
    protected implicit val ic: Composite[I]
    protected implicit val oc: Composite[O]

    def sql: String
    
    def stackFrame: Option[StackTraceElement]

    def process(a: A): Process[ConnectionIO, B] = 
      HC.process[O](sql, HPS.set(ai(a))).map(ob)

    def sink(a: A)(f: B => ConnectionIO[Unit]): ConnectionIO[Unit] =
      process(a).sink(f)

    def analysis: ConnectionIO[Analysis] =
      HC.prepareQueryAnalysis[I, O](sql)

    def to[F[_]](a: A)(implicit cbf: CanBuildFrom[Nothing, B, F[B]]): ConnectionIO[F[B]] =
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeQuery(HRS.buildMap[F,O,B](ob)))

    def list(a: A): ConnectionIO[List[B]] = 
      to[List](a)

    def vector(a: A): ConnectionIO[Vector[B]] = 
      to[Vector](a)

    def accumulate[F[_]: MonadPlus](a: A): ConnectionIO[F[B]] = 
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeQuery(HRS.accumulate[F, O].map(_.map(ob))))

    def unique(a: A): ConnectionIO[B] = 
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeQuery(HRS.getUnique[O])).map(ob)

    def option(a: A): ConnectionIO[Option[B]] = 
      HC.prepareStatement(sql)(HPS.set(ai(a)) *> HPS.executeQuery(HRS.getOption[O])).map(_.map(ob))

    def map[C](f: B => C): Query[A, C] =
      new Query[A, C] {
        protected type I = outer.I
        protected type O = outer.O
        protected val ai = outer.ai
        protected val ob = outer.ob andThen f
        protected val ic: Composite[I] = outer.ic 
        protected val oc: Composite[O] = outer.oc
        def sql = outer.sql
        def stackFrame = outer.stackFrame
      }

    def contramap[C](f: C => A): Query[C, B] =
      new Query[C, B] {
        protected type I = outer.I
        protected type O = outer.O
        protected val ai = outer.ai compose f
        protected val ob = outer.ob
        protected val ic: Composite[I] = outer.ic 
        protected val oc: Composite[O] = outer.oc
        def sql = outer.sql
        def stackFrame = outer.stackFrame
      }

    def toQuery0(a: A): Query0[B] =
      new Query0[B] {
        protected type O = outer.O
        protected val ob = outer.ob
        protected val oc: Composite[O] = outer.oc
        def sql = outer.sql
        def stackFrame = outer.stackFrame
        override def process = outer.process(a)
        override def sink(f: B => ConnectionIO[Unit]) = outer.sink(a)(f)
        override def to[F[_]](implicit cbf: CanBuildFrom[Nothing, B, F[B]]) = outer.to[F](a)
        override def list = outer.list(a)
        override def vector = outer.vector(a)
        override def accumulate[F[_]: MonadPlus] = outer.accumulate[F](a)  
        override def unique = outer.unique(a)
        override def option = outer.option(a)
      }

  }

  object Query {

    def apply[A, B](sql0: String, stackFrame0: Option[StackTraceElement])(implicit A: Composite[A], B: Composite[B]): Query[A, B] =
      new Query[A, B] {
        protected type I = A
        protected type O = B
        protected val ai: A => I = a => a
        protected val ob: O => B = o => o
        protected implicit val ic: Composite[I] = A
        protected implicit val oc: Composite[O] = B
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

  
  trait Query0[B] extends QueryDiagnostics { outer =>

    protected type O
    protected val ob: O => B
    protected implicit val oc: Composite[O]

    def sql: String
    
    def stackFrame: Option[StackTraceElement]

    def process: Process[ConnectionIO, B] = 
      HC.process[O](sql, ().point[PreparedStatementIO]).map(ob)

    def sink(f: B => ConnectionIO[Unit]): ConnectionIO[Unit] =
      process.sink(f)

    def analysis: ConnectionIO[Analysis] =
      HC.prepareQueryAnalysis0[O](sql)

    def to[F[_]](implicit cbf: CanBuildFrom[Nothing, B, F[B]]): ConnectionIO[F[B]] =
      HC.prepareStatement(sql)(HPS.executeQuery(HRS.buildMap[F,O,B](ob)))

    def list: ConnectionIO[List[B]] = 
      to[List]

    def vector: ConnectionIO[Vector[B]] = 
      to[Vector]

    def accumulate[F[_]: MonadPlus]: ConnectionIO[F[B]] = 
      HC.prepareStatement(sql)(HPS.executeQuery(HRS.accumulate[F, O].map(_.map(ob))))

    def unique: ConnectionIO[B] = 
      HC.prepareStatement(sql)(HPS.executeQuery(HRS.getUnique[O])).map(ob)

    def option: ConnectionIO[Option[B]] = 
      HC.prepareStatement(sql)(HPS.executeQuery(HRS.getOption[O])).map(_.map(ob))

    def map[C](f: B => C): Query0[C] =
      new Query0[C] {
        protected type O = outer.O
        protected val ob = outer.ob andThen f
        protected val oc: Composite[O] = outer.oc
        def sql = outer.sql
        def stackFrame = outer.stackFrame
      }

  }

  object Query0 {

    def apply[B](sql0: String, stackFrame0: Option[StackTraceElement])(implicit B: Composite[B]): Query0[B] =
      new Query0[B] {
        protected type O = B
        protected val ob: O => B = o => o
        protected implicit val oc: Composite[O] = B
        val sql = sql0
        val stackFrame = stackFrame0
      }

    implicit val queryFunctor: Functor[Query0] =
      new Functor[Query0] {
        def map[A, B](fa: Query0[A])(f: A => B) = fa map f
      }

  }

}
