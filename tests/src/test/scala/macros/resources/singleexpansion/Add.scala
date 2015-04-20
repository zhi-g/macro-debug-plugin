package macros.resources.singleexpansion

import macros.Arithmetics._

object Add {
  def foo(a: Int, b: Int): Double = {
    add(a, b)
  }
}