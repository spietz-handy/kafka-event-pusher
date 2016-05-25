organization := "com.handy"

name := "event-pusher"

version := "0.1"

isSnapshot := true

scalaVersion := "2.11.7"



mainClass in (Compile, packageBin) := Some("com.handy.kafka.EventPusher")

scalacOptions in Test ++= Seq("-Yrangepos")

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
  "org.scalaz.stream" %% "scalaz-stream" % "0.8",
  "org.http4s" % "http4s-core_2.11" % "0.13.2a",
  "org.http4s" % "http4s-dsl_2.11" % "0.13.2a",
  "org.http4s" % "http4s-client_2.11" % "0.13.2a",
  "org.http4s" % "http4s-blaze-client_2.11" % "0.13.2a",
  "org.http4s" % "http4s-circe_2.11" % "0.13.2a",
  "io.circe" %% "circe-core" % "0.4.1",
  "io.circe" %% "circe-generic" % "0.4.1",
  "io.circe" %% "circe-parser" % "0.4.1",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "slf4j-simple" % "1.7.10",
  "org.specs2" %% "specs2-core" % "3.8.3" % "test",
  "org.specs2" %% "specs2-mock" % "3.8.3" % "test"
)

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.handy-internal.com", "admin", "admin123")

publishTo := {
  val nexus = "http://nexus.handy-internal.com/nexus/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "content/repositories/releases")
}

// artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
//   val noSnap = artifact.name + "-" + artifact.classifier + module.revision
//   val snap = if (isSnapshot.value)
//     noSnap + "-SNAPSHOT"
//   else 
//     noSnap
//   snap + "." + artifact.extension
// }

version := {
  if (isSnapshot.value) 
    version.value + "-SNAPSHOT"
  else
    version.value
}

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)

assemblyJarName := {name.value + "-latest.jar"}

