lazy val commonSettings = Seq(
  organization := "io.sportadvisor",
  scalaVersion := "2.12.5",
  version := "0.1-alpha"
)
name := "SportAdvisor-Core"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"

val akkaHttpV = "10.0.11"
val scalaTestV = "3.0.4"
val slickVersion = "3.2.1"
val circeV = "0.9.1"
val sttpV = "1.1.5"

lazy val EndToEndTest = config("e2e") extend Runtime

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpV,
  "ch.megard" %% "akka-http-cors" % "0.2.2",
  "de.heikoseeberger" %% "akka-http-circe" % "1.19.0"
)

val dbDependencies = Seq(
  "org.postgresql" % "postgresql" % "42.1.4",
  "org.flywaydb" % "flyway-core" % "4.2.0",
  "com.zaxxer" % "HikariCP" % "2.7.0",
  "com.typesafe.slick" %% "slick" % slickVersion
)

val circeDependencies = Seq(
  "io.circe" %% "circe-core" % circeV,
  "io.circe" %% "circe-generic" % circeV,
  "io.circe" %% "circe-parser" % circeV,
  "io.circe" %% "circe-generic-extras" % circeV,
  "io.circe" %% "circe-java8" % circeV
)

val testDependencies = Seq(
  "com.softwaremill.sttp" %% "core" % sttpV % Test,
  "com.softwaremill.sttp" %% "akka-http-backend" % sttpV % Test,
  "org.scalatest" %% "scalatest" % scalaTestV % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
  "ru.yandex.qatools.embed" % "postgresql-embedded" % "2.9" % Test,
  "org.mockito" % "mockito-all" % "1.9.5" % Test
)

val e2eDependencies = Seq(
  "com.dimafeng" %% "testcontainers-scala" % "0.18.0" % EndToEndTest,
  "org.scalatest" %% "scalatest" % scalaTestV % EndToEndTest,
  "org.testcontainers" % "postgresql" % "1.7.3" % EndToEndTest,
  "org.scalaj" %% "scalaj-http" % "2.4.0" % EndToEndTest
)

val loggingDependencies = Seq(
  "tv.cntt" %% "slf4s-api" % "1.7.25",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.+",
  "ch.qos.logback" % "logback-core" % "1.3.0-alpha4",
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha4"
)

val mailDependencies = Seq(
  "ch.lightshed" %% "courier" % "0.1.4",
  "org.scalatra.scalate" %% "scalate-core" % "1.8.0"
)

val dependencies = Seq(
  "com.roundeights" %% "hasher" % "1.2.0",
  "com.pauldijou" %% "jwt-core" % "0.14.0",
  "com.github.pureconfig" %% "pureconfig" % "0.9.0",
  "com.wix" %% "accord-core" % "0.7.1",
  "tv.cntt" %% "scaposer" % "1.10"
)

libraryDependencies ++= dependencies ++ akkaDependencies ++ dbDependencies ++ circeDependencies ++ testDependencies ++ loggingDependencies ++ mailDependencies
mainClass in assembly := Some("io.sportadvisor.Application")
test in assembly := {}
assemblyJarName in assembly := "sportadvisor-api.jar"
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf"              => MergeStrategy.concat
  case x                             => MergeStrategy.first
}

scalastyleFailOnError := true
scalastyleFailOnWarning := true
wartremoverErrors in (Compile, compile) ++= Warts.unsafe
wartremoverErrors in (Compile, compile) ++= Seq(Wart.FinalCaseClass,
                                                Wart.Enumeration,
                                                Wart.Equals,
                                                Wart.LeakingSealed,
                                                Wart.Recursion)
wartremoverWarnings in (Compile, compile) ++= Warts.allBut(Wart.Nothing,
                                                           Wart.ImplicitParameter,
                                                           Wart.PublicInference)
coverageEnabled in Test := true
coverageEnabled in EndToEndTest := false

val e2eSettings =
  inConfig(EndToEndTest)(Defaults.testSettings) ++
    Seq(
      fork in EndToEndTest := false,
      parallelExecution in EndToEndTest := false,
      scalaSource in EndToEndTest := baseDirectory.value / "src/e2e/scala",
      resourceDirectory in EndToEndTest := baseDirectory.value / "src/e2e/resources",
      libraryDependencies ++= e2eDependencies
    )

val root = project
  .in(file("."))
  .configs(EndToEndTest)
  .settings(e2eSettings)

// docker build
enablePlugins(DockerPlugin)

imageNames in docker := Seq(
  ImageName("io.sportadvisor/sportadvisor-core:latest"),
  ImageName("io.sportadvisor/sportadvisor-core:it") // for integration test
)

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("java:8-jdk-alpine")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}
