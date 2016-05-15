organization := "com.handy"

name := "kafka-listener"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0",
  "org.scalaz.stream" %% "scalaz-stream" % "0.8",
  "org.http4s" % "http4s-core_2.11" % "0.13.2a",
  "org.http4s" % "http4s-dsl_2.11" % "0.13.2a",
  "org.http4s" % "http4s-client_2.11" % "0.13.2a",
  "org.http4s" % "http4s-blaze-client_2.11" % "0.13.2a"
)
