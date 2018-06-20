
val disableDocs = Seq[Setting[_]](
  sources in (Compile, doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false
)

val disablePublishing = Seq[Setting[_]](
  publishArtifact := false,
  skip in publish := true,
  publish := {},
  publishLocal := {}
)

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

val catsVersion = "1.1.0"
val catsEffectVersion = "1.0.0-RC2"

// Actual code for ocaps
lazy val core = (project in file("core"))
  .settings(scalaMacroDependencies)
  .settings(
    name := "ocaps-core",
    // https://mvnrepository.com/artifact/org.scala-lang/scala-reflect
    //libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.12.6",
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion  % Test,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test
  )

// Website documentation
// sbt "site/paradox"
lazy val site = (project in file("site"))
  .enablePlugins(ParadoxPlugin, ParadoxMaterialThemePlugin, ParadoxSitePlugin, GhpagesPlugin)
  .settings(disablePublishing)
  .settings(
    sourceDirectory in Paradox := sourceDirectory.value / "main" / "paradox",
    paradoxProperties in Paradox ++= Map("version" -> version.value),
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
    git.remoteRepo := "git@github.com:wsargent/ocaps.git",

    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25",
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion
  ).dependsOn(core)

// Slides for Scaladays
// sbt "slides/tut"
lazy val slides = (project in file("slides"))
  .enablePlugins(TutPlugin)
  .settings(disablePublishing)
  .settings(moduleName := "slides")
  .settings(
    tutSourceDirectory := baseDirectory.value / "tut",
    tutTargetDirectory := baseDirectory.value / "target" / "docs",
    watchSources ++= (tutSourceDirectory.value ** "*.html").get,

    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25",
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion,
    libraryDependencies += "eu.timepit" %% "refined"            % "0.9.0",
    libraryDependencies += "eu.timepit" %% "refined-cats"       % "0.9.0", // optional
  ).dependsOn(core)

lazy val root = (project in file(".")).settings(disablePublishing)
  .settings(
    name := "ocaps",

  // Settings that apply to all subprojects
  inThisBuild(List(
    organization := "ocaps",
    scalaVersion := "2.12.6",
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

    // define setting key to write configuration to .scalafmt.conf
    SettingKey[Unit]("scalafmtGenerateConfig") :=
      IO.write( // writes to file once when build is loaded
        file(".scalafmt.conf"),
        """style = IntelliJ
          |docstrings = JavaDoc
        """.stripMargin.getBytes("UTF-8")
      ),

    // https://github.com/sbt/sbt-header
    organizationName := "Will Sargent",
    startYear := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),

    scalacOptions in(Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings")
  )),

  // releasing
  //releaseCrossBuild := true,
  homepage := Some(url("https://github.com/wsargent/ocaps")),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishArtifact in Test := false,

  // publishing
  //  publishMavenStyle := true,
  //  pomIncludeRepository := { _ => false },
  //  publishTo := Some {
  //    val nexus = "https://oss.sonatype.org/"
  //    if (isSnapshot.value)
  //      "snapshots" at nexus + "content/repositories/snapshots"
  //    else
  //      "releases" at nexus + "service/local/staging/deploy/maven2"
  //  },

).dependsOn(core, slides, site)
  .aggregate(core, slides, site)
