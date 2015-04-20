import java.io._
import java.security.Permission

import com.sun.org.apache.bcel.internal.classfile._
import org.scalatest.FunSuite
import org.scalatest._

trait ExpansionsSuite extends FunSuite with Matchers {

  //source :
  def virtualizedOpen(body: => Unit): (Int, String) = {
    val outputStorage = new ByteArrayOutputStream()
    val outputStream = new PrintStream(outputStorage)
    case class SystemExitException(exitCode: Int) extends SecurityException
    val manager = System.getSecurityManager
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

  def runExpansionTest(testDir: File): File = {
    // val sources = testDir.listFiles().filter(_.getName.endsWith(".scala")).map(_.getAbsolutePath).toList
    val sources = List(testDir.getAbsolutePath)
    val cp = List("-cp", sys.props("sbt.paths.tests.classpath"))
    val debugPlugin = List("-Xplugin:" + sys.props("sbt.paths.plugin.jar"), "-Xplugin-require:macro-debug")
    val tempDir = File.createTempFile("temp", System.nanoTime.toString)
    tempDir.delete()
    tempDir.mkdir()
    val output = List("-d", tempDir.getAbsolutePath)
    val options = cp ++ debugPlugin ++ output ++ sources
    val (exitCode, stdout) = virtualizedOpen(scala.tools.nsc.Main.main(options.toArray))
    //println("The output of the compiler is:\n" + stdout)
    if (exitCode != 0) fail("The compiler has exited with code " + exitCode + ":\n" + stdout)
    tempDir
  }

  val resourceDir = new File(System.getProperty("sbt.paths.tests.macros") + File.separatorChar + "resources")
  val testDirs = resourceDir.listFiles().filter(_.listFiles().nonEmpty).filter(!_.getName.endsWith("_disabled"))


  def openRunOutput(pack: String, claz: String, clazOut: String): JavaClass = {
    val sourceImpl = testDirs.filter(_.getName == pack).head.listFiles().filter(_.getName == claz).head
    val out = runExpansionTest(sourceImpl).listFiles().filter(_.getName == "macros").head.listFiles().filter(_.getName == "resources")
      .head.listFiles().filter(_.getName == pack).head.listFiles()

    new ClassParser(new FileInputStream(out.filter(_.getName == clazOut)
      .head), out.filter(_.getName == clazOut).head.getName).parse()
  }

}


