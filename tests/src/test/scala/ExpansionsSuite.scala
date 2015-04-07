import java.io._
import java.security.Permission

import com.sun.org.apache.bcel.internal.classfile._
import com.sun.org.apache.bcel.internal.util.{ClassPath, SyntheticRepository}
import org.scalatest.FunSuite

class ExpansionsSuite extends FunSuite {

  def virtualizedOpen(body: => Unit): (Int, String) = {
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

  def runExpansionTest(testDir: File): File = {
    // val sources = testDir.listFiles().filter(_.getName.endsWith(".scala")).map(_.getAbsolutePath).toList
    val sources = List(testDir.getAbsolutePath)
    val cp = List("-cp", sys.props("sbt.paths.tests.classpath"))
    val debugPlugin = List("-Xplugin:" + sys.props("sbt.paths.plugin.jar"), "-Xplugin-require:macro-debug")
    val tempDir = File.createTempFile("temp", System.nanoTime.toString);
    tempDir.delete();
    tempDir.mkdir()
    val output = List("-d", tempDir.getAbsolutePath)
    val options = cp ++ debugPlugin ++ output ++ sources
    val (exitCode, stdout) = virtualizedOpen(scala.tools.nsc.Main.main(options.toArray))
    if (exitCode != 0) fail("The compiler has exited with code " + exitCode + ":\n" + stdout)
    //tempDir.listFiles().filter(_.
    // getName == "macros")(0).listFiles().filter(_.getName == "resources")(0).listFiles().filter(_.getName == testDir.getName).head
    println(tempDir.getAbsolutePath)
    tempDir
  }

  val resourceDir = new File(System.getProperty("sbt.paths.tests.macros") + File.separatorChar + "resources")
  val testDirs = resourceDir.listFiles().filter(_.listFiles().nonEmpty).filter(!_.getName().endsWith("_disabled"))

  /* testDirs.foreach(testDir => test(testDir.getName) {
     val outDir = runExpansionTest(testDir)
     outDir.listFiles().foreach {
       f =>
         val jc1: JavaClass = new ClassParser(new FileInputStream(f), f.getName).parse()
         val m1 = jc1.getMethods
         m1.foreach { meth =>
           val ln1 = meth.getLineNumberTable
         }

     }


   })*/

  def createJavaClass(): List[JavaClass] = {


    Nil
  }

  test("macroimpl/HelloWorld") {
    val sourceImpl = testDirs.filter(_.getName == "macroimpl")
      .head.listFiles().filter(_.getName == "HelloWorld.scala").head
    val out = runExpansionTest(sourceImpl).listFiles().filter(_.getName == "macros")
      .head.listFiles().filter(_.getName == "resources").head.listFiles().filter(_.getName == "macroimpl").head.listFiles()

    val jc1: JavaClass = new ClassParser(new FileInputStream(out.filter(_.getName == "HelloWorld$.class")
      .head), out.filter(_.getName == "HelloWorld$.class").head.getName).parse()

    assert(jc1.getMethods()(1).getLineNumberTable.toString === """LineNumber(0, 11), LineNumber(1, 1001), LineNumber(146, 11)""") //how to ignore PC numbers
  }

  test("helloworld/Test") {
    val sourceImpl = testDirs.filter(_.getName == "helloworld").head.listFiles().filter(_.getName == "Test.scala").head
    val out = runExpansionTest(sourceImpl).listFiles().filter(_.getName == "macros").head.listFiles().filter(_.getName == "resources").head.listFiles().filter(_.getName == "helloworld").head.listFiles()

    val jc1: JavaClass = new ClassParser(new FileInputStream(out.filter(_.getName == "Test$.class")
      .head), out.filter(_.getName == "Test$.class").head.getName).parse()
    for (x <- jc1.getMethods) println(x)
    assert(jc1.getMethods()(jc1.getMethods.length - 2).getLineNumberTable.toString === """LineNumber(0, 501)""")
  }

}
