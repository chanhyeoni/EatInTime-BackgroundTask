// name := "EatInTime-BackgroundTask"
// version := "1.0"
// scalaVersion := "2.11.8"

// import AssemblyKeys._

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")


lazy val root = (project in file(".")).
  settings(
    name := "EatInTime-BackgroundTask",
    version := "1.0",
    scalaVersion := "2.11.8",
    retrieveManaged := true,
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.0.0",
    libraryDependencies += "org.joda" % "joda-convert" % "1.5",
    libraryDependencies += "org.scala-lang" % "scala-library" % "2.11.8",
    libraryDependencies += "io.netty" % "netty-all" % "4.0.18.Final",
    libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.1.0",
    libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.1.0",
    libraryDependencies += "org.apache.spark" % "spark-mllib_2.11" % "2.1.0",
    libraryDependencies += "org.mongodb" %% "casbah" % "3.1.1",
    libraryDependencies += "org.mongodb.spark"	% "mongo-spark-connector_2.11" % "2.0.0",
    libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.0.0",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "1.0.0"
    )

assemblyMergeStrategy in assembly :=
{
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

enablePlugins(JavaAppPackaging)


// libraryDependencies ++= Seq(
// 	"org.joda" % "joda-convert" % "1.5",
// 	"org.scala-lang" % "scala-library" % "2.11.8",
// 	"io.netty" % "netty-all" % "4.0.18.Final",
// 	"org.apache.spark" % "spark-core_2.11" % "2.1.0",
// 	"org.apache.spark" % "spark-sql_2.11" % "2.1.0",
// 	"org.apache.spark" % "spark-mllib_2.11" % "2.1.0",
// 	"org.mongodb" %% "casbah" % "3.1.1",
// 	"org.mongodb.spark"	% "mongo-spark-connector_2.11" % "2.0.0",
// 	"org.scalaj" %% "scalaj-http" % "2.3.0",
// 	//"systems.fail-fast" %% "twilio-scala" % "0.2"
// 	//"org.scala-debugger" %% "scala-debugger-api" % "1.1.0-M3"
// )

// enablePlugins(AwsLambdaPlugin)