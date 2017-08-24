package doobie.syntax

import cats.data.Ior

final class AlignSyntax[A](as: List[A]) {
  def align[B](bs: List[B]): List[A Ior B] = {
    def go(as: List[A], bs: List[B], acc: List[A Ior B]): List[A Ior B] =
      (as, bs) match {
        case (a :: as, b :: bs) => go(as , bs , Ior.Both(a, b) :: acc)
        case (a :: as, Nil    ) => go(as , Nil, Ior.Left(a)    :: acc)
        case (Nil    , b :: bs) => go(Nil, bs , Ior.Right(b)    :: acc)
        case (Nil    , Nil    ) => acc.reverse
      }
    go(as, bs, Nil)
  }
}

trait ToAlignSyntax {
  implicit def toDoobieAlignSyntax[A](as: List[A]): AlignSyntax[A] =
    new AlignSyntax(as)
}

object align extends ToAlignSyntax
