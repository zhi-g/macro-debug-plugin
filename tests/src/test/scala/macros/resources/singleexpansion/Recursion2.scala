package macros.resources.singleexpansion

import macros.Macros._

object Recursion2 {
  def foo(): Int = {
    plus(1, 2)
  }
}