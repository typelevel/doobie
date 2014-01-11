package doobie
package util

import scalaz.effect.{ IO, MonadCatchIO, IORef }
import scalaz.syntax.effect.monadCatchIO._
import scalaz._
import Scalaz._

import argonaut._
import Argonaut._

final class TreeLogger[L] private (z: IORef[TreeLoc[TreeLogger.Node[L]]]) {
  import TreeLogger._, Node._

  def tree: IO[Tree[Node[L]]] =
    z.read.map(_.tree)

  def log[M[+_]: MonadCatchIO, A](s: => L, ma: M[A]): M[A] =
    for {
      p <- nanoTime.map(Pending(s, _)).liftIO[M]
      _ <- z.mod(_.insertDownLast(Tree(p))).liftIO[M]
      a <- ma.except(t => commit(p, t.left).liftIO[M])
      _ <- commit(p, a.right).liftIO[M]
    } yield a

  def dump(implicit ev: Show[L]): IO[Unit] = 
    tree >>= drawTree[L]

  ////// HELPERS

  private def nanoTime: IO[Long] =
    IO(System.nanoTime)

  private def commit[A](p: Pending[L], result: Throwable \/ A): IO[A] =
    for {
      e <- nanoTime.map(_ - p.start).map(Entry(p.label, result.bimap(Thrown.fromThrowable, _.toString), _))
      _ <- z.mod(_.setLabel(e).parent.get)
    } yield result.fold(throw _, identity)

}

object TreeLogger {

  def newLogger[L](rootLabel: L): IO[TreeLogger[L]] = 
    IO.newIORef(Tree[Node[L]](Node.Root(rootLabel)).loc).map(new TreeLogger(_))

  def drawTree[L: Show](t: Tree[Node[L]]): IO[Unit] =
    IO {
      val rows = t.draw.zipWithIndex.filter(_._2 % 2 == 0).map(_._1)
      val pipe = rows.map(_.lastIndexOf("|")).max
      val done = rows.map { s =>
        val (l, r) = s.splitAt(s.lastIndexOf('|'))
        l.isEmpty ? s | l.padTo(pipe, ' ') + r
      }
      println()
      done.foreach(s => println("  " + s))
      println()
    }

  sealed abstract class Node[L]
  object Node {  

    final case class Root[L](label: L) extends Node[L]
    final case class Pending[L](label: L, start: Long) extends Node[L]
    final case class Entry[L](label: L, result: Thrown \/ String, ns: Long) extends Node[L]

    // Sum type encoding as suggested by [mth] https://gist.github.com/markhibberd/8231912

    implicit def encodeNode[L: EncodeJson]: EncodeJson[Node[L]] =
      EncodeJson(_ match {
        case Root(l)        => Json("root"    := Json("label" := l))
        case Pending(l, s)  => Json("pending" := Json("label" := l, "start"  := s))
        case Entry(l, r, n) => Json("entry"   := Json("label" := l, "result" := r, "ns" := n))
      })
  
    implicit def decodeNode[L: DecodeJson]: DecodeJson[Node[L]] =
      DecodeJson(c =>
        tagged(c, "root", jdecode1L(Root.apply[L])("label"))|||
        tagged(c, "pending", jdecode2L(Pending.apply[L])("label", "start")) |||
        tagged(c, "entry", jdecode3L(Entry.apply[L])("label", "result", "ns")))

    private def tagged[A](c: HCursor, tag: String, decoder: DecodeJson[A]): DecodeResult[A] =
      (c --\ tag).hcursor.fold(DecodeResult.fail[A]("Invalid tagged type", c.history))(decoder.decode)

    implicit def showNode[L: Show]: Show[Node[L]] = {
      Show.shows { 
        case Root(l) => s"${Console.BOLD}${l.shows}${Console.RESET}"
        case Pending(l, s) => s"Pending: ${l.shows}"
        case Entry(l, \/-(a), n) => f"$ok ${l.shows.take(30)}%-30s| ${a.take(30)}%-30s  ${n / 1000000.0}%9.3f ms"
        case Entry(l, -\/(t), n) => f"$er ${l.shows.take(30)}%-30s| ${t.message.take(30)}%-30s  ${n / 1000000.0}%9.3f ms"
      }
    }
 
    val ok = s"${Console.GREEN}[ok]${Console.RESET}"
    val er = s"${Console.RED}[er]${Console.RESET}"

  }

}



