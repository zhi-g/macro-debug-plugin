package macros.resources.helloworld

import macros.HelloWorld._
import macros.Macros

object MultipleExpansionsInFile2 extends App {
  def foo(): Unit = {
    hello()
    hello()
    hello()
  }

  def foobar(): String ={
    val w1 = Macros.bar("hello")
    val w2 = Macros.bar("world")
    w1 + w2 + Macros.bar("plop")
  }

  def bar(): Unit = {
    hello()
  }
}