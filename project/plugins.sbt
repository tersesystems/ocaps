addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.8")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")

// slides
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.4")

// documentation
// I am futzing with diagrams, so if you want to build something then uncomment the
// following
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.3.5")
// and then comment out the line below:
//dependsOn(ProjectRef(file("/home/wsargent/work/paradox"), "plugin"))

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")

addSbtPlugin("io.github.jonas" % "sbt-paradox-material-theme" % "0.4.0")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.1")

addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.6.0-RC3")
