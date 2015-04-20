package macros.resources.singleexpansion

import macros.Macros._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object SingleExpansion {
  def foo(): String = {
    "a" + applyF(bar1, "hello")
  }

  def bar1(w: String): String = {
    w
  }
}