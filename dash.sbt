// > dash -s List
commands += {
  import scala.sys.process._
  import complete.DefaultParsers._
  val stuff = Seq(("-6", "java6",  "Java SE6"),
                  ("-7", "java7",  "Java SE7"),
                  ("-s", "scala",  "Scala"),
                  ("-z", "scalaz", "scalaz"))
  val option = stuff.map { case (o, d, _) => o ^^^ d } .reduceLeft(_ | _)
  val parser = token(Space ~> option) ~ token(Space ~> StringBasic)
  val help = Help.briefDetail(stuff.map { case (o, _, t) => (s"$o <word>", s"Search in $t") })
  Command("dash", help)(_ => parser) { case (state, (set, topic)) =>
    s"/usr/bin/open dash://$set:$topic".!
    state
  }
}
