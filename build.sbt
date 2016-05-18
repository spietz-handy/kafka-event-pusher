organization := "com.handy"

name := "event-pusher"

version := "0.1"

isSnapshot := true

scalaVersion := "2.11.7"



mainClass in (Compile, packageBin) := Some("com.handy.kafka.EventPusher")

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
  "org.scalaz.stream" %% "scalaz-stream" % "0.8",
  "org.http4s" % "http4s-core_2.11" % "0.13.2a",
  "org.http4s" % "http4s-dsl_2.11" % "0.13.2a",
  "org.http4s" % "http4s-client_2.11" % "0.13.2a",
  "org.http4s" % "http4s-blaze-client_2.11" % "0.13.2a",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "slf4j-simple" % "1.7.10"
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

artifact in (Compile, assembly) := {
  val art = (artifact in (Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in (Compile, assembly), assembly)
