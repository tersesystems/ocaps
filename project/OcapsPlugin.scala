import sbt.Keys._
import sbt._

object OcapsPlugin extends AutoPlugin {
  object autoImport {
    val apiVersion = taskKey[(Int, Int)]("Defines the API compatibility version for the project.")
  }

  // common settings that apply to all projects
  // http://blog.jaceklaskowski.pl/2015/04/12/using-autoplugin-in-sbt-for-common-settings-across-projects-in-multi-project-build.html
  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    organization := "ocaps",
    crossScalaVersions := Seq("2.12.6", "2.11.12"),
    scalaVersion := crossScalaVersions.value.head,

    //scalacOptions in(Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "utf-8",
      "-explaintypes",
      "-feature",
      "-language:existentials",
      "-language:dynamics",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfuture"
    ),

    libraryDependencies += scalaOrganization.value % "scala-reflect" % scalaVersion.value,
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        // if scala 2.11+ is used, quasiquotes are merged into scala-reflect
        case Some((2, scalaMajor)) if scalaMajor >= 11 => Seq()
        // in Scala 2.10, quasiquotes are provided by macro paradise
        case Some((2, 10)) =>
          Seq(
            compilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.patch),
            "org.scalamacros" %% "quasiquotes" % "2.1.0" cross CrossVersion.binary
          )
      }
    }
  )

  override def trigger = allRequirements

  val homepageUrl = "https://github.com/wsargent/ocaps"

  def extractApiVersion(version: String) = {
    val VersionExtractor = """(\d+)\.(\d+)\..*""".r
    version match {
      case VersionExtractor(major, minor) => (major.toInt, minor.toInt)
    }
  }

  def scaladocOptions(base: File, version: String, apiVersion: (Int, Int)): List[String] = {
    val sourceLoc =
      if (version.endsWith("SNAPSHOT")) {
        s"$homepageUrl/tree/master€{FILE_PATH}.scala"
      } else {
        val (major,minor) = apiVersion
        s"$homepageUrl/tree/v$major.$minor.0€{FILE_PATH}.scala"
      }

    val opts = List("-implicits",
      "-doc-source-url", sourceLoc,
      "-sourcepath", base.getAbsolutePath
    )
    opts
  }

  def isSnapshot(version: String): Boolean = version.endsWith("-SNAPSHOT")
}