name := """macro-plugin"""
version := "1.0"
scalaVersion := "2.11.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.11.1"

resourceDirectory in Compile <<= baseDirectory(_ / "src" / "main" / "scala" / "resources")