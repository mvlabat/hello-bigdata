name := "test_task"

version := "1.0"

scalaVersion := "2.11.8"

scalacOptions := Seq("-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val sprayV = "1.3.4"
  val akkaV = "2.4.12"
  val sparkV = "2.0.1"
  Seq(
    "io.spray"            %%  "spray-can"                   % sprayV withSources() withJavadoc(),
    "io.spray"            %%  "spray-client"                % sprayV withSources() withJavadoc(),
    "io.spray"            %%  "spray-routing"               % sprayV withSources() withJavadoc(),
    "com.typesafe.akka"   %%  "akka-actor"                  % akkaV,
    "org.apache.spark"    %%  "spark-core"                  % sparkV,
    "org.apache.spark"    %%  "spark-sql"                   % sparkV,
    "org.apache.commons"  %   "commons-email"               % "1.4",
    "org.mongodb.spark"   %   "mongo-spark-connector_2.11"  % "2.0.0"
  )
}

Seq(Twirl.settings: _*)