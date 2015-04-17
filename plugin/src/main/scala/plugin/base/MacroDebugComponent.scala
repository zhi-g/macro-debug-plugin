package plugin.base

import scala.meta.ui.Raw
import scala.reflect.internal.util.{OffsetPosition, RangePosition, BatchSourceFile}
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}
import scala.meta._
import scala.meta.internal.ast.{Term => ITerm}


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
                def mapPositions(reflectTree: Tree, metaTree: meta.Tree, newPosition: Position): Unit = {
                  val pos = if (newPosition.isRange) new RangePosition(syntheticSource, newPosition.start + metaTree.origin.start, newPosition.point + metaTree.origin.start, newPosition.end + metaTree.origin.start)
                  else new OffsetPosition(syntheticSource, newPosition.point + metaTree.origin.start)
                  reflectTree.setPos(pos)

                  (reflectTree, metaTree) match {
                    case (Apply(t1, t11), ITerm.Apply(t2, t21)) =>
                      mapPositions(t1, t2, newPosition)
                      t11.zip(t21).map(x => mapPositions(x._1, x._2, newPosition))
                    case (Select(t1, _), ITerm.Select(t2, t21)) =>
                      mapPositions(t1, t2, newPosition)

                    case (Block(t1, _), ITerm.Block(t2)) =>
                    // mapPositions(t1, t2, newPosition)
                    case (Ident(name1), ITerm.Name(name2)) => // no subnodes
                    case _ =>
                  }
                }

                val expansionString = showCode(tree)
                val nextOffset = syntheticSource.content.length + 1
                val shift = nextOffset - tree.pos.start
                syntheticSource = new BatchSourceFile(syntheticSource.file.canonicalPath, new String(syntheticSource.content) + "\n" + expansionString)
                // println("The exp string is " + expansionString)
                val metaTree = expansionString.replace("<unapply-selector>", "a").parse[Term]
                println("R is " + showRaw(tree))
                println("M is " + metaTree.show[Raw])

                val newPosition = if (tree.pos.isRange) {
                  new RangePosition(syntheticSource, tree.pos.start + shift, tree.pos.point + shift, tree.pos.end + shift)
                } else {
                  new OffsetPosition(syntheticSource, tree.pos.point + shift)
                }
                mapPositions(tree, metaTree, newPosition)
              //tree.setPos(newPosition)
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


  //doesn't work
  private def setPositionsToSubnodes(m: MacroExpansion, source: BatchSourceFile): Unit = {
    new Traverser {
      override def traverse(tree: Tree): Unit = {
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
