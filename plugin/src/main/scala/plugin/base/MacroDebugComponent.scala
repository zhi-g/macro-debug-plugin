package plugin.base

import scala.meta.ui.Raw
import scala.reflect.internal.util.{OffsetPosition, RangePosition, BatchSourceFile}
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.{Global, Phase}
import scala.meta._
import scala.meta.internal.ast.{Term => ITerm}
import scala.meta.internal.{ast => mTrees}


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
                val expansionString = showCode(tree)
                val nextOffset = syntheticSource.content.length + 1
                val shift = nextOffset - tree.pos.start
                syntheticSource = new BatchSourceFile(syntheticSource.file.canonicalPath, new String(syntheticSource.content) + "\n" + expansionString)
                // println("The exp string is " + expansionString)
                val metaTree = expansionString.replace("<unapply-selector>", "a").parse[Term]
                //println("R is " + showRaw(tree))
                //println("M is " + metaTree.show[Raw])

                val newPosition = if (tree.pos.isRange) {
                  new RangePosition(syntheticSource, tree.pos.start + shift, tree.pos.point + shift, tree.pos.end + shift)
                } else {
                  new OffsetPosition(syntheticSource, tree.pos.point + shift)
                }
                println("Start mapping positions")
                mapPositions(tree, metaTree, newPosition)
              //tree.setPos(newPosition)
              //setPositionsToSubnodes(MacroExpansion(tree, newPosition, shift), syntheticSource)
              case _ =>
                super.traverse(tree)
            }
          }
        }.traverse(tree)

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

            case (Block(t1, t11), ITerm.Block(t2)) =>
              (t1 ::: t11 :: Nil).zip(t2).map(x => mapPositions(x._1, x._2, newPosition))
            case (TypeApply(t1, t11), ITerm.ApplyType(t2, t21)) =>
              mapPositions(t1, t2, newPosition)
              t11.zip(t21).map(x => mapPositions(x._1, x._2, newPosition))

            case (ClassDef(_, _, _, impl), mTrees.Defn.Class(_, _, _, _, tmpl)) =>
              mapPositions(impl, tmpl, newPosition)

            case (Typed(expr, tpt), ITerm.Ascribe(expr1, tpt1)) =>
              mapPositions(expr, expr1, newPosition)
              mapPositions(tpt, tpt1, newPosition)
            case (ValDef(mods, name, tpt, rhs), mTrees.Defn.Val(mmods, mname, mtpt, mrhs)) =>
              // mapPositions(tpt, mtpt, newPosition)
              mtpt match {
                case Some(t) => mapPositions(tpt, t, newPosition)
                case None =>
              }
              mapPositions(rhs, mrhs, newPosition)
            case (Template(rparents, rself, rbody), mTrees.Template(early, parents, self, stats)) =>
              rparents.zip(parents).map(x => mapPositions(x._1, x._2, newPosition))
              mapPositions(rself, self, newPosition)
              stats match {
                case Some(s) => rbody.zip(s).map(x => mapPositions(x._1, x._2, newPosition))
                case _ =>
              }
            case (DefDef(mods, name, tparams, vparams, tpt, rhs), mTrees.Defn.Def(mmods, mname, mtparams, mparams, decltype, mbody)) =>
              decltype match {
                case Some(t) => mapPositions(tpt, t, newPosition)
                case None =>
              }
              mapPositions(rhs, mbody, newPosition)
              tparams.zip(mtparams).map(x => mapPositions(x._1, x._2, newPosition))
              vparams.zip(mparams).map { x =>
                x._1.zip(x._2).map(y => mapPositions(y._1, y._2, newPosition))
              }

            case (Ident(name1), ITerm.Name(name2)) =>
            case (Literal(_), _) =>
            case (TypeTree(), _) =>
            case (noSelfType, _) =>
            case _ =>
              println("The next reflect tree is " + showRaw(reflectTree))
              println("The next meta tree is" + metaTree.show[Raw])
          }
        }
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
