import sbt._
import sbt.Keys._

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
    sharedSettings: _*
    ) settings(
    resourceDirectory in Compile <<= baseDirectory(_ / "src" / "main" / "scala" / "resources"),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-library" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),

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
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-library" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
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
