package macros.resources.helloworld

import macros.Macros._

object RecursionExpansions {
  def foo(a: Int, b: Int): Int = {
    recursive(a,b)
  }
}