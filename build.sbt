name := "Schematic2Blueprint"

version := "0.1"

scalaVersion := "2.10.3"

mainClass in (Compile, run) := Some("schematic.Schematic2Blueprint")

parallelExecution in Test := false

libraryDependencies ++= Seq(
    "com.seaglasslookandfeel" % "seaglasslookandfeel" % "0.2",
    "org.apache.commons" % "commons-io" % "1.3.2"
)
