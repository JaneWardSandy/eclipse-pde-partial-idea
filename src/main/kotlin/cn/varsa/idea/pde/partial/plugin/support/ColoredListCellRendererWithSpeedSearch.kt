package cn.varsa.idea.pde.partial.plugin.support

import com.intellij.ui.*
import com.intellij.ui.speedSearch.*
import javax.swing.*

class ColoredListCellRendererWithSpeedSearch<T>(private val appender: ColoredListCellRenderer<T>.(list: JList<out T>, value: T?, index: Int, selected: Boolean, hasFocus: Boolean) -> Unit) :
  ColoredListCellRenderer<T>() {

  constructor(appender: ColoredListCellRenderer<T>.(value: T?) -> Unit) : this({ _, value, _, _, _ ->
                                                                                 appender(this, value)
                                                                               })

  override fun customizeCellRenderer(
    list: JList<out T>, value: T?, index: Int, selected: Boolean, hasFocus: Boolean
  ) {
    appender(this, list, value, index, selected, hasFocus)
    SpeedSearchUtil.applySpeedSearchHighlighting(list, this, false, selected)
  }

  companion object {
    fun stringRender(): ColoredListCellRenderer<Any> = ColoredListCellRendererWithSpeedSearch { value ->
      value?.toString()?.also { append(it) }
    }
  }
}
