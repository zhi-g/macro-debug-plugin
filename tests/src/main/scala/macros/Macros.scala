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

  def plus(m: Int, n: Int): Int = macro plus_recursive_impl

  def plus_recursive_impl(c: Context)(m: c.Tree, n: c.Tree): c.Tree = {
    import c.universe._
    val q"${m_value: Int}" = m
    val q"${n_value: Int}" = n
    if (n_value == 0) q"$m"
    else q"plus($m_value + 1, $n_value - 1)"
  }

  def applyF(f: String => String, word: String): String = macro applyF_impl

  def applyF_impl(c: Context)(f: c.Tree, word: c.Tree): c.Tree = {
    import c.universe._
    q"""
      $f($word)
      """
  }

  def printz(param: Any): Unit = macro printz_impl

  def printz_impl(c: Context)(param: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    val paramRep = showCode(param.tree)
    val paramRepTree = Literal(Constant(paramRep))
    val paramRepExpr = c.Expr[String](paramRepTree)
    reify {
      println(paramRepExpr.splice + " = " + param.splice)
    }
  }

}
