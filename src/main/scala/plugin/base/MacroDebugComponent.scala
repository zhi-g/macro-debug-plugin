package plugin.base

import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}
import scala.reflect.runtime.{universe => ru}

/**
 * Created by zhivka on 19.02.15.
 */
class MacroDebugComponent(val global: Global) extends PluginComponent {

  import global._

  override val runsAfter = List("typer")
  override val description = "generates synthetic code for macro expansions"
  //override val runsRightAfter = List("typer") //not sure if should do this instead
  val phaseName = "macro-debug"

  override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    override def apply(unit: CompilationUnit): Unit = {
       //TODO change the name, this doesnt fit
      def findMacroExpansionsAndGenerateCode(tree: Tree) = {
        // this is certainly a BatchSourceFile, see Global.newSourceFile
        val source = unit.source.asInstanceOf[BatchSourceFile]
        //get number of lines in the original file, will change over time
        var linesInFile = source.calculateLineIndices(source.content).length
        var code = new String(source.content)


        new Traverser {
          override def traverse(tree: Tree): Unit = {

            tree.attachments.get[analyzer.MacroExpansionAttachment] match {
              case Some(analyzer.MacroExpansionAttachment(expandee: Tree, expanded: Tree)) =>
                  val expansionString = showCode(expanded)
                  //in what order the macros appear in the AST? This shouldn't depend on that, but does for now
                  //Position of the synthetic code in the file will be xi where x = position of the macro in the original file and i = line number of code in the macro
                  val posInFile = source.offsetToLine(expandee.pos.start)
                 // val e = expanded.pos
                  //need to append empty lines until the line of beginning the macro
                  //Bad assumption for the line numbers, can easily find cases where this doesn't work. Also there is the implicit assumption that no macro is larger than 100 lines
                  var emptyLines = '\n'*(posInFile*100 - linesInFile) //empty lines:is it ok with this or ... ? not sure need to check
                  // TODO set the position of expandee to startOffset = code.length+emptyLines, endOffset = stratOffset + expandee.pos.end - expandee.pos.start
                  code +=emptyLines
                  code += expansionString
                  linesInFile = posInFile*100 + expansionString.count(x => x == '\u000A' || x == '\u000D') //not sure if it'd work, need to test

              case _ => //do nothing

            }

            /*val macroAttachment = if (tree.attachments.get[java.util.HashMap[String, Any]].isDefined) {
              tree.attachments.get[java.util.HashMap[String, Any]].map {
                att => (att.get("expandeeTree").asInstanceOf[Tree], att.get("expansionString").asInstanceOf[String])
              }
            }else None*/
            super.traverse(tree) //continue with traversing
          }

        }.traverse(tree)

        new BatchSourceFile(source.file.toString(), code)
      }

     /* Should be partially done in traverse since there we can access the trees to change their positions
     def generate(macroExpansions: List[(Tree, Tree)]): BatchSourceFile = {
        val source: BatchSourceFile = unit.source.asInstanceOf[BatchSourceFile] // this is certainly a BatchSourceFile, see Global.newSourceFile
        val expansionString = global.showCode(expanded) // the String that I should add at the end of the
        val macroPos = tree.pos
        val linesToBeAdded  = macroPos.end - macroPos.start //number of line needed for this macro expansion at the end of the file
        source.calculateLineIndices(source.content).length //get number of lines in the original file
        val code = new String(source.content)
        ???
      }*/

      val sourceWithExpansions = findMacroExpansionsAndGenerateCode(unit.body)
     // val content = findMacroExpansionsAndGenerateCode(unit.body)
      val instanceMirror = ru.runtimeMirror(unit.source.content.getClass.getClassLoader).reflect(unit.source)
      instanceMirror.reflectField(ru.typeOf[BatchSourceFile].decl(ru.TermName("source")).asMethod).set(sourceWithExpansions) // messedup
      // unit.source.getClass.getField("source").set(unit.source, sourceWithExpansions)
    }
  }
}
