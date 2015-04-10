package macros.resources.helloworld

import macros.Macros._

object MultipleExpansionsInLine extends App {
  def fooOne(): String = {
    foo("hello") + foo(" ") + foo("world")
  }

  def barOne(): String = {
    bar("hello")
  }

  def barTwo(): String = {
    bar("world") + "\n" + bar("hello") + bar("plop")
  }

}