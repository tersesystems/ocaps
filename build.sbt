
// https://github.com/typelevel/cats/blob/master/build.sbt#L610

val tutPath = settingKey[String]("Path to tut files")

val stableVersion = settingKey[String]("The version of ocaps that we want the user to download.")
stableVersion := "0.2.0"

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
  .settings(
    name := "ocaps",

    // scaladoc settings
    scalacOptions in (Compile, doc) ++= Seq(
      "-doc-title", name.value,
      "-doc-version", version.value,
      "-doc-footer", "",
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
      "version" -> stableVersion.value,
      "snip.examples.base_dir" -> s"${(sourceDirectory in Test).value}/scala/ocaps/examples",
    ),
    paradoxMaterialTheme in Compile ~= {
      _.withSocial(
        uri("https://github.com/wsargent"),
        uri("https://twitter.com/will_sargent")
      ).withRepository(uri("https://github.com/wsargent/ocaps"))
      .withCustomStylesheet("https://use.fontawesome.com/releases/v5.1.0/css/all.css")
    },
    paradoxDirectives += MermaidDirective,
    // https://github.com/lightbend/paradox/issues/139
    sourceDirectory in Paradox in paradoxTheme := sourceDirectory.value / "paradox" / "_template",

    ParadoxMaterialThemePlugin.paradoxMaterialThemeSettings(Paradox),

    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25" % Test,
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
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
    // tut is great, but we don't need to run it every time we preview the site.
    addMappingsToSiteDir(tut, tutPath),
    libraryDependencies += "eu.timepit" %% "refined" % "0.9.0" % "tut",

    // https://github.com/sbt/sbt-header
    organizationName := "Will Sargent",
    startYear := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),

    homepage := Some(url("https://github.com/wsargent/ocaps")),

    publishArtifact in Test := false,
    releaseCrossBuild := true
  )