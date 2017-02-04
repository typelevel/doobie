package doobie.util

// embarrassigly minimal lens implementation to avoid a lib conflict
object lens {

  case class Lens[A, B](get: A => B, set: (A, B) => A) {

    def andThen[C](bc: Lens[B, C]): Lens[A, C] =
      Lens(a => bc.get(get(a)), (a, c) => set(a, bc.set(get(a), c)))

    def >=>[C](bc: Lens[B, C]): Lens[A, C] =
      andThen(bc)

    def compose[C](bc: Lens[C, A]): Lens[C, B] =
      bc andThen this

    def modify(a: A, f: B => B): A =
      set(a, f(get(a)))

    def xmapA[AA](f: AA => A, g: A => AA): Lens[AA, B] =
      Lens(aa => get(f(aa)), (aa, b) => g(set(f(aa), b)))

    def xmapB[BB](f: BB => B, g: B => BB): Lens[A, BB] =
      Lens(a => g(get(a)), (a, bb) => set(a, f(bb)))

  }

  type @>[A,B] = Lens[A,B]

}
