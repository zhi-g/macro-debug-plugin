package plugin.base

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.{PluginComponent, Plugin}

/**
 * Created by zhivka on 17.02.15.
 */
class MacroDebugPlugin(val global: Global) extends Plugin {

  override val name = "macro-plugin"
  override val description = "generates synthetic code for macro expansion debugging"
  override val components = List[PluginComponent](new MacroDebugComponent(this.global))


}