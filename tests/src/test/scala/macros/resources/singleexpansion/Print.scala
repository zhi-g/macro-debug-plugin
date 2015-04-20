package macros.resources.singleexpansion

import macros.PrintTree._


object Print {
  def foo(): String = {
    print("hello" == "world")
  }
}