package macros.resources.helloworld

import macros.HelloWorld._

object MultipleExpansionsInFile extends App {
  def foo(): Unit = {
    hello()
    hello()
    hello()
  }

  def bar(): Unit = {
    hello()
  }
}