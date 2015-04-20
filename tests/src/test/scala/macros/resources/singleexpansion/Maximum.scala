package macros.resources.singleexpansion

import macros.Arithmetics._

object Maximum {
  def foo(a: Int, b: Int): Double = {
    val list = List(42.0, 24.42)
    maximum(list)
  }
}