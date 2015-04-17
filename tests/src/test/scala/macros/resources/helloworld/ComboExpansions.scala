package macros.resources.helloworld

import macros.Macros
import macros.Macros._

import scala.language.experimental.macros


object ComboExpansions {
  def foo(): String = {
    val str = new StringBuilder()
    for (i <- 0 to 100) {
      str.append(bar(i + "")) // 7 lines of expansion + 1 empty line
    }

    val str2 = Macros.foo(str.toString())
    var sum = 0

    for (i <- 1 to 100) {
      sum += plus(1, 10) // 1 line of expansion + 1 empty line
    }

    val t = sum + str2 + bar("hello") // (7+1) lines of expansion
    t
  }
}