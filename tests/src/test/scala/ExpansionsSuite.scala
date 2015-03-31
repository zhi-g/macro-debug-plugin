import java.io.{File, PrintStream, ByteArrayOutputStream}
import java.security.Permission

import org.scalatest.FunSuite


class ExpansionsSuite extends FunSuite {

  def virtualizedPopen(body: => Unit): (Int, String) = {
    val outputStorage = new ByteArrayOutputStream()
    val outputStream = new PrintStream(outputStorage)
    case class SystemExitException(exitCode: Int) extends SecurityException
    val manager = System.getSecurityManager()
    System.setSecurityManager(new SecurityManager {
      override def checkPermission(permission: Permission): Unit = ()

      override def checkPermission(permission: Permission, context: AnyRef): Unit = ()

      override def checkExit(exitCode: Int): Unit = throw new SystemExitException(exitCode)
    })
    try {
      scala.Console.withOut(outputStream)(scala.Console.withErr(outputStream)(body)); throw new Exception("failed to capture exit code")
    }
    catch {
      case SystemExitException(exitCode) => outputStream.close(); (exitCode, outputStorage.toString)
    }
    finally System.setSecurityManager(manager)
  }

  def runExpansionTest(testDir: File): Unit = {
    val sources = testDir.listFiles().filter(_.getName.endsWith(".scala")).map(_.getAbsolutePath).toList
    val cp = List("-cp", sys.props("sbt.paths.tests.classpath"))
    val debugPlugin = List("-Xplugin:" + sys.props("sbt.paths.plugin.jar"), "-Xplugin-require:macro-plugin")
    val tempDir = File.createTempFile("temp", System.nanoTime.toString);
    tempDir.delete();
    tempDir.mkdir()
    val output = List("-d", tempDir.getAbsolutePath)
    val options = cp ++ debugPlugin ++ output ++ sources
    val (exitCode, stdout) = virtualizedPopen(scala.tools.nsc.Main.main(options.toArray))
    if (exitCode != 0) fail("The compiler has exited with code " + exitCode + ":\n" + stdout)

  }

  val resourceDir = new File(System.getProperty("sbt.paths.tests.macros") + File.separatorChar + "resources")
  val testDirs = resourceDir.listFiles().filter(_.listFiles().nonEmpty).filter(!_.getName().endsWith("_disabled"))

  testDirs.foreach(testDir => test(testDir.getName)(runExpansionTest(testDir)))


}
