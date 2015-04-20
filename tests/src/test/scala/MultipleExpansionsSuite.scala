import com.sun.org.apache.bcel.internal.classfile.Method
import com.sun.org.apache.bcel.internal.util.{ClassPath, SyntheticRepository}
import org.scalatest.FunSuite
import org.scalatest._

class MultipleExpansionsSuite extends ExpansionsSuite {

  test("multipleexpansions/MultipleExtensionsInFile") {
    val jc1 = openRunOutput("multipleexpansions", "MultipleExpansionsInFile.scala", "MultipleExpansionsInFile$.class")
    val methFoo = jc1.getMethods.filter(m => m.getName == "foo")(0)
    val lineNumberTable = methFoo.getLineNumberTable //
    lineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 17\), LineNumber\(.+, 19\), LineNumber\(.+, 21\)"""
  }

  test("multipleexpansions/MultipleExpansionsInLine") {
    val jc1 = openRunOutput("multipleexpansions", "MultipleExpansionsInLine.scala", "MultipleExpansionsInLine$.class")
    val methods = jc1.getMethods
    val methFoo = methods.filter(m => m.getName == "fooOne")(0)
    val lineNumberTable = methFoo.getLineNumberTable
    lineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      //"LineNumber(0, 7), LineNumber(7, 20), LineNumber(12, 22), LineNumber(17, 24), LineNumber(22, 7)
      """LineNumber\(.+, 7\), LineNumber\(.+, 20\), LineNumber\(.+, 22\), LineNumber\(.+, 24\), LineNumber\(.+, 7\)"""
    val methBar1 = methods.filter(m => m.getName == "barOne")(0)
    methBar1.getLineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 11\), LineNumber\(.+, 26\)""" //This expansion has 8 lines
    val methBar2 = methods.filter(m => m.getName == "barTwo")(0)
    methBar2.getLineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 15\), LineNumber\(.+, 34\), LineNumber\(.+, 15\), LineNumber\(.+, 42\), LineNumber\(.+, 15\), LineNumber\(.+, 50\), LineNumber\(.+, 15\)"""
  }

  test("multipleexpansions/MultipleExpansionsInFile2") {
    val jc1 = openRunOutput("multipleexpansions", "MultipleExpansionsInFile2.scala", "MultipleExpansionsInFile2$.class")
    val methods = jc1.getMethods

    val methFoo = jc1.getMethods.filter(m => m.getName == "foo")(0)
    val lineNumberTable = methFoo.getLineNumberTable //
    lineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 24\), LineNumber\(.+, 26\), LineNumber\(.+, 28\)"""

    val methFooBar = methods.filter(m => m.getName == "foobar")(0)
    methFooBar.getLineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 14\), LineNumber\(.+, 15\), LineNumber\(.+, 16\), LineNumber\(.+, 46\), LineNumber\(.+, 16\)"""
    //ln 30, 38 are for the expansions of val w1, w2

  }



  test("multipleexpansions/ComboExpansions") {
    val jc1 = openRunOutput("multipleexpansions", "ComboExpansions.scala", "ComboExpansions$.class")
    val methFoo = jc1.getMethods.filter(_.getName == "foo").head
    methFoo.getLineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 11\), LineNumber\(.+, 12\), LineNumber\(.+, 16\), LineNumber\(.+, 17\), LineNumber\(.+, 19\), LineNumber\(.+, 23\), LineNumber\(.+, 40\), LineNumber\(.+, 23\), LineNumber\(.+, 24\)"""
  }
}


