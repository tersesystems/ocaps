addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.0.15")

// documentation
// I am futzing with diagrams, so if you want to build something then uncomment the
// following
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.10.7")
// and then comment out the line below:
//dependsOn(ProjectRef(file("/home/wsargent/work/paradox"), "plugin"))

addSbtPlugin("com.github.sbt" % "sbt-site-paradox" % "1.7.0")

addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.8.0")

addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
