package compiler

import scala.tools.nsc.Global
import scala.tools.reflect.ToolBox

/**
 * Created by zhivka on 15.03.15.
 */
object Compiler {
  val addPlugin = "-Xplugin:" + "target/scala-2.11/macro-plugin-assembly-1.0.jar"
  val tb = scala.reflect.runtime.currentMirror.mkToolBox(options = " " + addPlugin)

  //add classpath

  val rootMirror = scala.reflect.runtime.universe.rootMirror //it's the rootMirror of Global?

  def compile(code: String) = {
    tb.compile(tb.parse(code))
  }

}
