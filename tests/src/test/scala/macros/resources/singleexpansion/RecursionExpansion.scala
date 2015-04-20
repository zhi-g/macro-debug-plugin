package macros.resources.singleexpansion

import macros.Macros._

object RecursionExpansion {
  def foo(): String = {
    "a" + plus(1, 2)
  }
}