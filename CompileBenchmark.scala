package scripts

import bleep.*
import bleep.commands.SourceGen
import bleep.model.{CrossProjectName, ProjectName}
import typo.*
import typo.internal.generate
import typo.internal.sqlfiles.readSqlFileDirectories

import java.nio.file.Path
import java.sql.{Connection, DriverManager}

object CompileBenchmark extends BleepScript("CompileBenchmark") {
  val buildDir = Path.of(sys.props("user.dir"))

  case class Result(
      lib: String,
      crossId: model.CrossId,
      inlineImplicits: Boolean,
      fixVerySlowImplicit: Boolean,
      avgtime: Long,
      mintime: Long,
      times: List[Long]
  )

  override def run(started: Started, commands: Commands, args: List[String]): Unit = {
    implicit val c: Connection = DriverManager.getConnection(
      "jdbc:postgresql://localhost:6432/Adventureworks?user=postgres&password=password"
    )
    val metadb = MetaDb.fromDb(TypoLogger.Noop)

    val crossIds = List(
      "jvm212",
      "jvm213",
      "jvm3"
    ).map(str => model.CrossId(str))
    val variants = List(
      ("baseline (only case class)", None, Nil, "typo-tester-none", true),
      ("zio-jdbc", Some(DbLibName.ZioJdbc), Nil, "typo-tester-zio-jdbc", true),
      ("doobie (with fix)", Some(DbLibName.Doobie), Nil, "typo-tester-doobie", true),
      ("doobie (without fix)", Some(DbLibName.Doobie), Nil, "typo-tester-doobie", false),
      ("anorm", Some(DbLibName.Anorm), Nil, "typo-tester-anorm", true),
      ("zio-json", None, List(JsonLibName.ZioJson), "typo-tester-zio-jdbc", true),
      ("circe", None, List(JsonLibName.Circe), "typo-tester-doobie", true),
      ("play-json", None, List(JsonLibName.PlayJson), "typo-tester-anorm", true)
    )
    val GeneratedAndCheckedIn = Path.of("generated-and-checked-in")

    val results: List[Result] =
      variants.flatMap { case (lib, dbLib, jsonLib, projectName, fixVerySlowImplicit) =>
        val targetSources = buildDir.resolve(projectName).resolve(GeneratedAndCheckedIn)

        List(false, true).flatMap { inlineImplicits =>
          generate(
            Options(
              pkg = "adventureworks",
              dbLib,
              jsonLib,
              enableDsl = dbLib.nonEmpty,
              enableTestInserts = Selector.All,
              inlineImplicits = inlineImplicits,
              fixVerySlowImplicit = fixVerySlowImplicit,
              enableStreamingInserts = false
            ),
            metadb,
            ProjectGraph(
              name = "",
              targetSources,
              Selector.ExcludePostgresInternal, // All
              readSqlFileDirectories(TypoLogger.Noop, buildDir.resolve("adventureworks_sql")),
              Nil
            )
          ).foreach(_.overwriteFolder())

          crossIds.map { crossId =>
            started.projectPaths(CrossProjectName(ProjectName(projectName), Some(crossId))).sourcesDirs.fromSourceLayout.foreach { p =>
              started.logger.warn(s"Deleting $p because tests will not compile")
              bleep.internal.FileUtils.deleteDirectory(p)
            }

            val desc = s"${crossId.value}, lib=$lib, inlineImplicits=$inlineImplicits, fixVerySlowImplicit=$fixVerySlowImplicit"
            println(s"START $desc")
            val times = 0.to(2).map { _ =>
              val crossProjectName = model.CrossProjectName(model.ProjectName(projectName), Some(crossId))
              commands.clean(List(crossProjectName))
              SourceGen(false, Array(crossProjectName)).run(started)
              time(commands.compile(List(crossProjectName)))
            }
            val avgtime = times.sum / times.length
            val mintime = times.min
            val res = Result(
              lib = lib,
              crossId = crossId,
              inlineImplicits = inlineImplicits,
              fixVerySlowImplicit = fixVerySlowImplicit,
              avgtime = avgtime,
              mintime = mintime,
              times = times.toList
            )
            println(res)
            res
          }
        }
      }

    results foreach println
  }

  def time(run: => Unit): Long = {
    val start = System.currentTimeMillis()
    run
    System.currentTimeMillis() - start
  }
}
