
val stableVersion = settingKey[String]("The version of ocaps that we want the user to download.")
stableVersion := "1.0.0"

val catsVersion = "2.12.0"
val catsEffectVersion = "3.5.7"

lazy val root = (project in file("."))
  .enablePlugins(ParadoxPlugin) // https://developer.lightbend.com/docs/paradox/current/index.html
  .enablePlugins(ParadoxSitePlugin) // https://www.scala-sbt.org/sbt-site/generators/paradox.html
  .enablePlugins(SitePreviewPlugin) // https://www.scala-sbt.org/sbt-site/generators/paradox.html
  .enablePlugins(SiteScaladocPlugin) // https://www.scala-sbt.org/sbt-site/api-documentation.html#scaladoc
  .enablePlugins(ScalaUnidocPlugin) // https://github.com/sbt/sbt-unidoc#how-to-unify-scaladoc
  .enablePlugins(GhpagesPlugin) // https://github.com/sbt/sbt-ghpages
  .settings(
    name := "ocaps",
    
    // scaladoc settings
    scalacOptions in (Compile, doc) ++= Seq(
      "-doc-title", name.value,
      "-doc-version", version.value,
      "-doc-footer", "",
      "-sourcepath", (sourceDirectory in Compile).value.getPath, // needed for scaladoc to strip the location of the linked source path
      "-doc-source-url", s"https://github.com/tersesystems/ocaps/blob/${version.value}/ocaps/src/main€{FILE_PATH}.scala",
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
    paradoxProperties ++= Map(
      "version" -> stableVersion.value,
      "snip.examples.base_dir" -> s"${(sourceDirectory in Test).value}/scala/ocaps/examples",
    ),
    paradoxDirectives += MermaidDirective,
    paradoxTheme := Some(builtinParadoxTheme("generic")),
    paradoxRoots := List("index.html"),

    libraryDependencies += "org.slf4j" % "slf4j-api" % "2.0.16" % Test,
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.12" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion % Test,

    git.remoteRepo := "git@github.com:tersesystems/ocaps.git",

    // https://github.com/sbt/sbt-header
    organizationName := "Terse Systems",
    startYear := Some(2018),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),

    homepage := Some(url("https://github.com/tersesystems/ocaps")),

    publishArtifact in Test := false,
    releaseCrossBuild := true
  )