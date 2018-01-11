lazy val framework = (project in file("framework"))
lazy val example = (project in file("example")).dependsOn(framework).aggregate(framework)

lazy val root = (project in file(".")).settings(
  name := "scala-capabilities",
  inThisBuild(
    scalaVersion:= "2.12.4"
  )
).aggregate(example, framework).dependsOn(example, framework)

addCommandAlias("runExample", ";project example; run")