import com.sun.org.apache.bcel.internal.classfile.Method
import com.sun.org.apache.bcel.internal.util.{ClassPath, SyntheticRepository}
import org.scalatest.FunSuite
import org.scalatest._

class SingleExpansionsSuite extends ExpansionsSuite {

  test("Test") {
    val jc1 = openRunOutput("singleexpansion", "Test.scala", "Test$.class")
    val methFoo: Method = jc1.getMethods.filter(m => m.getName == "foo")(0)
    val lineNumberTable = methFoo.getLineNumberTable

    lineNumberTable.toString should fullyMatch regex """LineNumber\(.+, 12\)"""
  }

  test("Recursion1") {
    val jc1 = openRunOutput("singleexpansion", "RecursionExpansion.scala", "RecursionExpansion$.class")
    val methRecursion = jc1.getMethods.filter(m => m.getName == "foo")(0)
    methRecursion.getLineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 7\), LineNumber\(.+, 11\), LineNumber\(.+, 7\)"""
  }
  test("Recursion2") {
    val jc1 = openRunOutput("singleexpansion", "Recursion2.scala", "Recursion2$.class")
    val methRecursion = jc1.getMethods.filter(m => m.getName == "foo")(0)
    methRecursion.getLineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 11\)"""
  }

  test("SingleExpansion") {
    val jc1 = openRunOutput("singleexpansion", "SingleExpansion.scala", "SingleExpansion$.class")
    val methRecursive = jc1.getMethods.filter(m => m.getName == "foo")(0)
    methRecursive.getLineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 10\), LineNumber\(.+, 18\), LineNumber\(.+, 10\)"""
  }

  test("Add") {
    val jc1 = openRunOutput("singleexpansion", "Add.scala", "Add$.class")
    val methFoo = jc1.getMethods.filter(m => m.getName == "foo").head
    methFoo.getLineNumberTable.toString should fullyMatch regex
      """LineNumber\(.+, 11\)"""
  }

  test("Print") {
    val jc1 = openRunOutput("singleexpansion", "Print.scala", "Print$.class")
    val methFoo = jc1.getMethods.filter(m => m.getName == "foo").head
    methFoo.getLineNumberTable.toString should fullyMatch regex
      """LineNumber\(.+, 12\)"""
  }
  test("Maximum") {
    val jc1 = openRunOutput("singleexpansion", "Maximum.scala", "Maximum$.class")
    val methFoo = jc1.getMethods.filter(m => m.getName == "foo").head
    methFoo.getLineNumberTable.toString.replaceAll("\n", "") should fullyMatch regex
      """LineNumber\(.+, 7\), LineNumber\(.+, 8\), LineNumber\(.+, 13\), LineNumber\(.+, 14\), LineNumber\(.+, 18\)"""

    /* LineNumberTable:
        line 7: 0  - create val
        line 8: 28 - load val
        line 13: 38 - macro, why not 12?
        line 14: 40
        line 18: 53
     */
  }

}


