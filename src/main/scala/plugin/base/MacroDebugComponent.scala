package plugin.base

import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.runtime.{universe => ru}
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

/**
 * Created by zhivka on 19.02.15.
 */
class MacroDebugComponent(val global: Global) extends PluginComponent {

  import global._

  import scala.reflect.internal.util.Position

  override val runsAfter = List("typer")
  override val description = "generates synthetic code for macro expansions"

  val phaseName = "macro-debug"

  override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    override def apply(unit: CompilationUnit): Unit = {
      def findMacroExpansionsAndGenerateCode(tree: Tree) = {
        // this is certainly a BatchSourceFile, see Global.newSourceFile
        val source = unit.source.asInstanceOf[BatchSourceFile]
        //get number of lines in the original file, will change over time
        var linesInFile = source.calculateLineIndices(source.content).length
        var code = new String(source.content)

        new Traverser {
          override def traverse(tree: Tree): Unit = {
            tree.attachments.get[analyzer.MacroExpansionAttachment] match {
              case Some(a@analyzer.MacroExpansionAttachment(expandee: Tree, expanded: Tree)) =>
                // println(analyzer.isMacroImplRef(tree))
                val expansionString = showCode(expanded)
                val posInFile = source.offsetToLine(tree.pos.start)
                //need to append empty lines until the line of beginning the macro
                //Bad assumption for the line numbers, can easily find cases where this doesn't work. Also there is the implicit assumption that no macro is larger than 100 lines
                var emptyLines = "\n" * (posInFile * 100 - linesInFile)
                code += emptyLines
                code += expansionString
                linesInFile = posInFile * 100 + expansionString.count(x => x == '\u000A' || x == '\u000D')
                // set the position of expandee to startOffset = code.length+emptyLines = posInFile*100
                expandee.setPos(Position.offset(source, /*source.lineToOffset(*/ posInFile * 100))
              //this will not work for now as the position we want to set is in the new file and the offset
              // doesn't exist in this one. will need to traverse one more time
              // the tree to set all the positions correctly

              case _ =>

            }
            super.traverse(tree) //continue with traversing
          }

        }.traverse(tree)

        println(code)
        new BatchSourceFile(source.file.toString(), code)
      }

      val sourceWithExpansions = findMacroExpansionsAndGenerateCode(unit.body)
      val sourceTerm = ru.typeOf[CompilationUnit].decl(ru.TermName("source")).asMethod
      val instanceMirror = scala.reflect.runtime.currentMirror.reflect(unit)
      instanceMirror.reflectField(sourceTerm).set(sourceWithExpansions)
    }
  }
}
