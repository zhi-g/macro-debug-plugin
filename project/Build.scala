import sbt._
import sbt.Keys._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._

object Build extends Build {
  lazy val sharedSettings = Defaults.coreDefaultSettings ++ Seq(
    scalaVersion := "2.11.6",
    version := "2.1.0-SNAPSHOT",
    publishArtifact in Test := false,
    scalacOptions ++= Seq("-deprecation", "-feature"),
    parallelExecution in Test := false, // hello, reflection sync!!
    logBuffered := false,
    scalaHome := {
      val scalaHome = System.getProperty("scala.home")
      if (scalaHome != null) {
        println(s"Going for scala home at $scalaHome")
        Some(file(scalaHome))
      } else None
    }
  )

  lazy val plugin = Project(
    id = "macros-plugin",
    base = file("plugin")
  ) settings (
    sharedSettings ++ assemblySettings: _*
    ) settings(
    resourceDirectory in Compile <<= baseDirectory(_ / "src" / "main" / "scala" / "resources"),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    libraryDependencies += "org.scalameta" % "scalahost" % "0.1.0-SNAPSHOT" cross CrossVersion.full,
    test in assembly := {},
    mergeStrategy in assembly := {
      case "scalac-plugin.xml" => MergeStrategy.first
      case x =>
        val oldStrategy = (mergeStrategy in assembly).value
        oldStrategy(x)
    },
    logLevel in assembly := Level.Error,
    jarName in assembly := "macro_debug" + "_" + scalaVersion.value + "-" + version.value + "-assembly.jar",
    assemblyOption in assembly ~= { _.copy(includeScala = false) },
    Keys.`package` in Compile := {
      val slimJar = (Keys.`package` in Compile).value
      val fatJar = new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      slimJar
    },
    packagedArtifact in Compile in packageBin := {
      val temp = (packagedArtifact in Compile in packageBin).value
      val (art, slimJar) = temp
      val fatJar = new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      (art, slimJar)
    }
    )

  lazy val usePluginSettings = Seq(
    scalacOptions in Compile <++= (Keys.`package` in(plugin, Compile)) map { (jar: File) =>
      System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
      val addPlugin = "-Xplugin:" + jar.getAbsolutePath
      // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
      // add plugin timestamp to compiler options to trigger recompile of
      // main after editing the plugin. (Otherwise a 'clean' is needed.)
      val dummy = "-Jdummy=" + jar.lastModified
      Seq(addPlugin, dummy)
    }
  )

  lazy val tests = Project(
    id = "tests",
    base = file("tests")
  ) settings (
    sharedSettings ++ usePluginSettings: _ *
    ) settings(
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test",
    unmanagedSourceDirectories in Test <<= (scalaSource in Test) { (root: File) =>
      val (_ :: Nil, others) = root.listFiles.toList.partition(_.getName == "macros")
      System.setProperty("sbt.paths.tests.macros", root.listFiles.toList.filter(_.getName == "macros").head.getAbsolutePath)
      others
    },
    fullClasspath in Test := {
      val testcp = (fullClasspath in Test).value.files.map(_.getAbsolutePath).mkString(java.io.File.pathSeparatorChar.toString)
      sys.props("sbt.paths.tests.classpath") = testcp
      (fullClasspath in Test).value
    }
    )
}
