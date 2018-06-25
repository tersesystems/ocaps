
// https://github.com/typelevel/cats/blob/master/build.sbt#L610
lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
  libraryDependencies += scalaOrganization.value % "scala-reflect" % scalaVersion.value % "provided",
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
val tutPath = settingKey[String]("Path to tut files")

val catsVersion = "1.1.0"
val catsEffectVersion = "1.0.0-RC2"

lazy val root = (project in file("."))
  .enablePlugins(ParadoxPlugin) // https://developer.lightbend.com/docs/paradox/current/index.html
  .enablePlugins(ParadoxSitePlugin) // https://www.scala-sbt.org/sbt-site/generators/paradox.html
  .enablePlugins(ParadoxMaterialThemePlugin) // https://jonas.github.io/paradox-material-theme/getting-started.html
  .enablePlugins(SiteScaladocPlugin) // https://www.scala-sbt.org/sbt-site/api-documentation.html#scaladoc
  .enablePlugins(ScalaUnidocPlugin) // https://github.com/sbt/sbt-unidoc#how-to-unify-scaladoc
  .enablePlugins(TutPlugin) // http://tpolecat.github.io/tut//configuration.html
  .enablePlugins(GhpagesPlugin) // https://github.com/sbt/sbt-ghpages
  .settings(scalaMacroDependencies)
  .settings(
    name := "ocaps",
    organization := "ocaps",

    crossScalaVersions := Seq("2.12.6", "2.11.12"),
    scalaVersion := crossScalaVersions.value.head,

    scalacOptions in ThisBuild ++= Seq(
      "-deprecation",
      "-encoding", "utf-8",
      "-explaintypes",
      "-feature",
      "-language:existentials",
      "-language:experimental.macros",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfuture"
    ),
    //scalacOptions in(Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),

    // scaladoc settings
    scalacOptions in (Compile, doc) ++= Seq(
      "-doc-title", name.value,
      "-doc-version", version.value,
      "-doc-footer", "Slick is developed by Typesafe and EPFL Lausanne.",
      "-sourcepath", (sourceDirectory in Compile).value.getPath, // needed for scaladoc to strip the location of the linked source path
      "-doc-source-url", s"https://github.com/wsargent/ocaps/blob/${version.value}/ocaps/src/main€{FILE_PATH}.scala",
      "-implicits",
      "-diagrams", // requires graphviz
      "-groups"
    ),

    autoAPIMappings := true,
    //    siteSubdirName in SiteScaladoc := {
    //      val (major, minor) = apiVersion
    //      "api/$major.minor"
    //    },
    //    apiURL in doc := {
    //      val (major, minor) = apiVersion.value
    //      Some(url(s"https://wsargent.github.io/ocaps/api/${major}.${minor}"))
    //    },
    // siteSourceDirectory := target.value / "generated-stuff",

    // paradox settings
    paradoxProperties in Paradox ++= Map(
      "version" -> version.value,
      "snip.examples.base_dir" -> s"${(sourceDirectory in Test).value}/scala/ocaps/examples",
    ),
    paradoxMaterialTheme in Compile ~= {
      _.withRepository(uri("https://github.com/wsargent/ocaps"))
    },
    paradoxMaterialTheme in Compile ~= {
      _.withSocial(
        uri("https://github.com/wsargent"),
        uri("https://twitter.com/will_sargent")
      )
    },
    ParadoxMaterialThemePlugin.paradoxMaterialThemeSettings(Paradox),

    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion % "tut, test",

    git.remoteRepo := "git@github.com:wsargent/ocaps.git",

    // define setting key to write configuration to .scalafmt.conf
    SettingKey[Unit]("scalafmtGenerateConfig") :=
      IO.write( // writes to file once when build is loaded
        file(".scalafmt.conf"),
        """style = IntelliJ
          |docstrings = JavaDoc
        """.stripMargin.getBytes("UTF-8")
      ),

    // slides settings
    tutPath := "slides",
    tutSourceDirectory := baseDirectory.value / tutPath.value,
    tutTargetDirectory := baseDirectory.value / "target" / tutPath.value,
    watchSources ++= (tutSourceDirectory.value ** "*.html").get,
    addMappingsToSiteDir(tut, tutPath),
    libraryDependencies += "eu.timepit" %% "refined" % "0.9.0" % "tut",

    // https://github.com/sbt/sbt-header
    organizationName := "Will Sargent",
    startYear := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),

    homepage := Some(url("https://github.com/wsargent/ocaps")),
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    publishArtifact in Test := false,
    releaseCrossBuild := true

  )