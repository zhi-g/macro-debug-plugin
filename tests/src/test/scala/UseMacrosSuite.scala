
import org.scalatest.FunSuite
import macros.HelloWorld

class UseMacrosSuite extends FunSuite {

  import Test._

  test("Hello world") {
    f()
  }
}

object Test extends App {
  def f() {
    HelloWorld.hello()
  }
}