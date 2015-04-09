package plugin.base

import scala.reflect.internal.util.{OffsetPosition, RangePosition, BatchSourceFile}
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}
import scala.meta._

/**
 * Created by zhivka on 19.02.15.
 */
class MacroDebugComponent(val global: Global) extends PluginComponent {

  import global._

  implicit val c = Scalahost.mkGlobalContext(global)

  import scala.reflect.internal.util.Position

  override val runsAfter = List("typer")
  override val description = "generates synthetic code and positions for macro expansion debugging"

  val phaseName = "macro-debug-gen"

  /**
   * Positions of the generated macro code will be created from the original position
   * of the macro application. If the macro application is at line x, then the generated
   * code for that macro will start at position x*assumedMacroLength
   */
  case class MacroExpansion(expandee: Tree, newPosition: Position, shift: Int)

  override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {

    override def apply(unit: CompilationUnit): Unit = {
      def appendExpansions(tree: Tree) = {
        // see Global.newSourceFile
        var syntheticSource = unit.source.asInstanceOf[BatchSourceFile]
        
        new Traverser {
          override def traverse(tree: Tree): Unit = {
            tree.attachments.get[analyzer.MacroExpansionAttachment] match {
              case Some(a@analyzer.MacroExpansionAttachment(expandee: Tree, expanded: Tree)) =>

                val expansionString = "\n" + showCode(tree)
                val nextOffset = syntheticSource.content.length + 1
                val shift = nextOffset - tree.pos.start
                syntheticSource = new BatchSourceFile(syntheticSource.file.canonicalPath, new String(syntheticSource.content) + expansionString)

                if (tree.pos == NoPosition) {
                  println("Tree in traverser without position")
                }


                val newPosition = if (tree.pos.isRange) {
                  new RangePosition(syntheticSource, tree.pos.start + shift, tree.pos.point + shift, tree.pos.end + shift)
                } else {
                  new OffsetPosition(syntheticSource, tree.pos.point + shift)
                }
                tree.setPos(newPosition)
              //setPositionsToSubnodes(MacroExpansion(tree, newPosition, shift), syntheticSource)
              case _ =>
                super.traverse(tree)


            }
          }
        }.traverse(tree)

      }
      appendExpansions(unit.body)
    }
  }

  private def setPositionsToSubnodes(m: MacroExpansion, source: BatchSourceFile): Unit = {
    new Traverser {
      override def traverse(tree: Tree): Unit = {
        // println("Position is " + tree.pos)

        tree.pos match {
          case p: RangePosition =>
            tree.setPos(new RangePosition(source, p.start + m.shift, p.point + m.shift, p.end + m.shift))
          case p: OffsetPosition =>
            tree.setPos(new OffsetPosition(source, p.point + m.shift))
          case _ =>
            tree.setPos(new OffsetPosition(source, m.newPosition.point))
        }
        super.traverse(tree)
      }
    }.traverse(m.expandee)
  }

}
