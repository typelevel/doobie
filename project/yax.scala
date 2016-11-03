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

  def walk(src: File, destDir: File, flags: Set[String]): List[File] =
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
    } else {
      try {
        src.listFiles.toList.flatMap(f => walk(f, new File(destDir, src.getName), flags))
      } catch {
        case n: NullPointerException => Nil
      }
    }

  // TODO obey [[sbt.Keys.crossPaths]] setting
  private def srcDirs(root: File, config: String, sbv: String) =
    List("", s"-$sbv").map { postfix =>
      root / s"/src/$config/scala$postfix"
    }

  private def foo(root: File, config: String, flags: String*) = Def.task {
    val dest = sourceManaged.value
    val sbv = scalaBinaryVersion.value
    val srcs = srcDirs(root, config, sbv)
    srcs.flatMap(walk(_, dest, flags.toSet))
  }

  // all non-hidden files
  private def closure(src: File): List[File] =
    if (src.isFile) {
      if (src.isHidden) Nil else List(src)
    } else {
      src.listFiles.toList.flatMap(closure)
    }

  def apply(root: File, flags: String*): Seq[Setting[_]] =
    inConfig(Compile)(Seq(sourceGenerators += foo(root, "main", flags: _*).taskValue)) ++
    inConfig(Test)(Seq(sourceGenerators += foo(root, "test", flags: _*).taskValue)) ++
    Seq(watchSources := watchSources.value ++ closure(root))

}
