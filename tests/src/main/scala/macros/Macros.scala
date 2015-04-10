package macros

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context


object Macros {
  def foo(word: String): String = macro foo_impl

  def foo_impl(c: Context)(word: c.Tree): c.Tree = {
    import c.universe._

    q"""
     	$word
    """
  }

  def bar(word: String): String = macro bar_impl

  def bar_impl(c: Context)(word: c.Tree): c.Tree = {
    import c.universe._
    q"""
        if ($word == "hello"){
          $word + " world!"
        } else if ($word == "world") {
          "Hello " + $word
        } else {
          "Unknown word" + $word
        }
    """
  }

  def recursive(m: Int, n: Int): Int = macro recursive_impl
  def recursive_impl(c: Context)(m: c.Tree, n: c.Tree): c.Tree = {
    import c.universe._
    val q"${m_value: Int}" = m
    val q"${n_value: Int}" = n
    if (n_value == 0) q"$m"
    else q"recursive($m_value + 1, $n_value - 1)"
  }
}
