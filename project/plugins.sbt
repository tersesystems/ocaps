addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.0.15")

// documentation
// I am futzing with diagrams, so if you want to build something then uncomment the
// following
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.9.2")
// and then comment out the line below:
//dependsOn(ProjectRef(file("/home/wsargent/work/paradox"), "plugin"))

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.3")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")
