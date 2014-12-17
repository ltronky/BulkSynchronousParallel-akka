name := """TesiBSPScala"""

val akkaVersion = "2.3.7"

fork in run := true

version := "0.1"

scalaVersion := "2.11.4"

resolvers += Resolver.sonatypeRepo("public")

scalacOptions in Compile ++= Seq(
  "-encoding",
  "UTF-8",
  "-target:jvm-1.6",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
  "com.github.scopt"  %% "scopt"        % "3.2.0",
  "org.fusesource"    %  "sigar"        % "1.6.4"
)

javaOptions in run ++= Seq(
  "-Djava.library.path=./sigar",
  "-Xms128m", "-Xmx1024m",
  "-Dcom.sun.management.jmxremote.port=9999",
  "-Dcom.sun.management.jmxremote.authenticate=false",
  "-Dcom.sun.management.jmxremote.ssl=false",
  "-XX:+UnlockCommercialFeatures",
  "-XX:+FlightRecorder")

assemblyJarName in assembly := "BSPClusterNode.jar"

test in assembly := {}

mainClass in assembly := Some("it.unipd.trluca.bsp.Main")
