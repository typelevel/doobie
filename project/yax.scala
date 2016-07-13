import sbt._
import sbt.Keys._
import java.io._
import scala.io.Source

object yax {

  private def process(file: File, lines: List[String], flags: Set[String]): List[String] = {
    def go(lines: List[(String, Int)], out: List[String], stack: List[String]): List[String] =
      lines match {

        // No more lines, done!
        case Nil => 
          if (stack.isEmpty) out.reverse
          else sys.error(s"$file: EOF: expected ${stack.map(s => s"#-$s").mkString(", ")}")

        // Push a token.
        case (s, _) :: ss if s.startsWith("#+") =>
          go(ss, out, s.drop(2).trim :: stack)

        // Pop a token.
        case (s, n) :: ss if s.startsWith("#-") => 
          val tok  = s.drop(2).trim
          val line = n + 1
          stack match {
            case `tok` :: ts => go(ss, out, ts)
            case t :: _      => sys.error(s"$file: $line: expected #-$t, found #-$tok")
            case _           => sys.error(s"$file: $line: unexpected #-$tok")
          }

        // Add a line, or not, depending on tokens.
        case (s, _) :: ss => 
          if (stack.forall(flags)) go(ss, s :: out, stack)
          else                     go(ss,      out, stack)

      }
    go(lines.zipWithIndex, Nil, Nil)
  }

  private def walk(src: File, destDir: File, flags: Set[String]): List[File] =
    if (src.isFile) {
      if (src.isHidden) Nil
      else {
        val f = new File(destDir, src.getName)
        val s = Source.fromFile(src, "UTF-8")
        try {
          destDir.mkdirs()
          val pw = new PrintWriter(f, "UTF-8")
          try {
            process(src, s.getLines.toList, flags).foreach(pw.println)
          } finally {
            pw.close()
          }
        } finally {
          s.close()
        }
        List(f)
      }
    } else
      src.listFiles.toList.flatMap(f => walk(f, new File(destDir, src.getName), flags))

  private def foo(src: File, flags: String*) = Def.task {
    walk(src, sourceManaged.value, flags.toSet)
  }

  def apply(root: File, flags: String*): Seq[Setting[_]] =
    inConfig(Compile)(Seq(sourceGenerators += foo(root / "/src/main/scala", flags: _*).taskValue)) ++
    inConfig(Test   )(Seq(sourceGenerators += foo(root / "/src/test/scala", flags: _*).taskValue))

}