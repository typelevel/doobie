package doobie.syntax

import doobie.util.transactor.Transactor
import doobie.util.yolo.Yolo

object transactor {

  class TransactorOps[A](a: A) {
    def yolo[M[_]](implicit ev: Transactor[M, A]): Yolo[M, A] =
      new Yolo(a)
  }

}