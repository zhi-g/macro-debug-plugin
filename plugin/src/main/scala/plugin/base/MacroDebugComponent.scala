package plugin.base

import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}

//import scala.sprinter.printers.PrettyPrinters


/**
 * Created by zhivka on 19.02.15.
 */
class MacroDebugComponent(val global: Global) extends PluginComponent {

  import global._

  import scala.reflect.internal.util.Position

  override val runsAfter = List("typer")
  override val description = "generates synthetic code for macro expansions"

  val phaseName = "macro-debug"
  /*
   * Positions of the generated macro code will be created from the original position
   * of the macro application. If the macro application is at line x, then the generated
   * code for that macro will start at position x*assumedMacroLength
   */
  val assumedMacroLength = 100

  /*
   *
   */
  case class MacroExpansion(expandee: Tree, originalLineInFile: Int, posInLine: Int)

  override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {

    override def apply(unit: CompilationUnit): Unit = {
      def appendExpansions(tree: Tree) = {
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
                //don't need to touch the expandee and expanded, just do operations on tree
                val expansionString = showCode(tree)
                val posInFile = source.offsetToLine(tree.pos.start)
                val emptyLines = Array.fill((posInFile + 1) * assumedMacroLength - linesInFile + 1)('\n') //lines start at 0
                code.appendAll(emptyLines)
                code.append(expansionString)
                linesInFile = (posInFile + 1) * assumedMacroLength + expansionString.count(x => x == '\u000A' || x == '\u000D')
                expansionsDetected = MacroExpansion(tree, posInFile, 0) :: expansionsDetected

              case _ =>
                super.traverse(tree)

            }
          }
        }.traverse(tree)

        val synSource = new BatchSourceFile(source.file.canonicalPath, code)
        setPositionsToExpansions(expansionsDetected, synSource)
      }
      appendExpansions(unit.body)
    }
  }

  private def setPositionsToExpansions(expansions: List[MacroExpansion], synSource: BatchSourceFile): Unit = {
    for (mExpansion <- expansions) {
      setPositionsToExpansions(mExpansion.expandee.children.map {
        a =>
          if (a.pos != NoPosition && mExpansion.expandee.pos != NoPosition) {
            MacroExpansion(a, mExpansion.originalLineInFile, a.pos.start - mExpansion.expandee.pos.start)
          } else {
            MacroExpansion(a, mExpansion.originalLineInFile, 0)
          }
      }, synSource)
      mExpansion.expandee.setPos(Position.offset(synSource, synSource.lineToOffset((mExpansion.originalLineInFile) * assumedMacroLength) + mExpansion.posInLine))
    }
  }

  private def setPositionsTrees(t: Tree, source: BatchSourceFile): Unit = {
    if (t.pos != NoPosition) {
      t.setPos(Position.offset(source, t.pos.point))
    } else {
    }
    for (c <- t.children) setPositionsTrees(c, source)
  }

  private def setPositionsToExpanded(expanded: List[MacroExpansion], synSource: BatchSourceFile): Unit = {
    for (x <- expanded) {
      x.expandee.setPos(Position.offset(synSource, synSource.lineToOffset(x.originalLineInFile * assumedMacroLength + 1)))
    }
  }

}
