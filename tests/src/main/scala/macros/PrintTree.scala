package macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object PrintTree {
  def print(tree: Any): String = macro PrintTreeMacro.print
}

class PrintTreeMacro(val c: Context) {

  import c.universe._

  def print(tree: Tree): Tree = {
    val str = "The tree is : "
    val code = showCode(tree)
    q"$str + $code"

  }

}
