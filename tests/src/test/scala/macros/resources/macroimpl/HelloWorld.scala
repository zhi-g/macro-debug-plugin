package macros.resources.macroimpl

import scala.language.experimental.macros
import scala.reflect.macros._

object HelloWorld {
  def hello(): Unit = macro hello_impl

  def hello_impl(c: whitebox.Context)(): c.Expr[Unit] = {
    import c.universe._
    c.Expr( q""" println ("Hello World!") """)
  }
}