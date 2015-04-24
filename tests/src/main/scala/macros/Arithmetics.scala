package macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object Arithmetics {

  def add(a: Double, b: Double): Double = macro add_impl

  def add_impl(c: Context)(a: c.Tree, b: c.Tree): c.Tree = {
    import c.universe._
    q"$a + $b"
  }

  def maximum(a: List[Double]): Double = macro maximum_impl

  def maximum_impl(c: Context)(a: c.Tree): c.Tree = {
    import c.universe._
    q"""var max = $a.head
        $a.foreach { x =>
          if ( x>max) max = x
        }
        max
     """
  }

  def subAbs(a: Double, b: Double): Double = macro sub_impl

  def sub_impl(c: Context)(a: c.Tree, b: c.Tree): c.Tree = {
    import c.universe._
    q"""
        if ($a > $b) $a-$b else $b-$a
    """
  }

}
