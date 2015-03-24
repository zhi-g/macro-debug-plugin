package plugin.base

import scala.reflect.internal.util.{OffsetPosition, BatchSourceFile}
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

  case class MacroExpansion(expandee: Tree, originalLineInFile: Int, offInLine: Int)

  override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    override def apply(unit: CompilationUnit): Unit = {

      def appendExpansions(tree: Tree): BatchSourceFile = {
        // see Global.newSourceFile
        val source = unit.source.asInstanceOf[BatchSourceFile]
        //get number of lines in the original file, will change over time
        val code = new StringBuilder()
        code.append(new String(source.content))

        var linesInFile = source.calculateLineIndices(source.content).length
        var expansionsDetected = List[MacroExpansion]()

        new Traverser {
          override def traverse(tree: Tree): Unit = {
            tree.attachments.get[analyzer.MacroExpansionAttachment] match {
              case Some(a@analyzer.MacroExpansionAttachment(expandee: Tree, expanded: Tree)) =>
                //println(tree + " is detected as macro extension" + showCode(tree))
                val expansionString = showCode(expanded)
                val posInFile = source.offsetToLine(tree.pos.start) + 1 //lines start at 0
              //need to append empty lines until the line of beginning the macro
              //Bad assumption for the line numbers, can easily find cases where this doesn't work. Also there is the implicit assumption that no macro is larger than 100 lines
              val emptyLines = "\n" * (posInFile * 100 - linesInFile)
                code.append(emptyLines)
                if (emptyLines.length != 0) {
                  code.append(expansionString)
                  linesInFile = posInFile * 100 + expansionString.count(x => x == '\u000A' || x == '\u000D')
                  expansionsDetected = MacroExpansion(expandee, posInFile, 0) :: expansionsDetected
                }
              case _ => // do nothing
            }
            super.traverse(tree)
          }

        }.traverse(tree)

        val synSource = new BatchSourceFile(source.file.canonicalPath, code)
        setPositionsExpansions(expansionsDetected, synSource)
        setPositionsTrees(tree, synSource)

        synSource

      }

      def setPositionsExpansions(expansions: List[MacroExpansion], synSource: BatchSourceFile): Unit = {
        for (mExpansion <- expansions) {
          setPositionsExpansions(mExpansion.expandee.children.map(a => MacroExpansion(a, mExpansion.originalLineInFile, a.pos.start - mExpansion.expandee.pos.start)), synSource)
          mExpansion.expandee.setPos(Position.offset(synSource, synSource.lineToOffset(mExpansion.originalLineInFile * 100) + mExpansion.offInLine))

        }

      }

      def setPositionsTrees(t: Tree, source: BatchSourceFile): Unit = {
        if (t.pos != NoPosition) {
          t.setPos(Position.offset(source, t.pos.point))
        } else {

        }
        for (c <- t.children) setPositionsTrees(c, source)

      }

      val sourceWithExpansions = appendExpansions(unit.body)
      val sourceTerm = ru.typeOf[CompilationUnit].decl(ru.TermName("source")).asMethod
      val instanceMirror = scala.reflect.runtime.currentMirror.reflect(unit)
      instanceMirror.reflectField(sourceTerm).set(sourceWithExpansions)


    }
  }
}
