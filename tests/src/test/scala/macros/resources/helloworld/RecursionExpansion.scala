package macros.resources.helloworld

import macros.Macros._

object RecursionExpansion {
  def foo(): String = {
    "a" + plus(1, 2)
  }
}