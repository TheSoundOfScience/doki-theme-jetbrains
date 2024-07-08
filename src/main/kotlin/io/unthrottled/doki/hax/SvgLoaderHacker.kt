package io.unthrottled.doki.hax

import io.unthrottled.doki.hax.svg.SVGLoader
import io.unthrottled.doki.hax.svg.SvgElementColorPatcherProvider
import io.unthrottled.doki.icon.ColorPatcher
import io.unthrottled.doki.service.PluginService
import io.unthrottled.doki.themes.DokiTheme
import io.unthrottled.doki.util.runSafely
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*


object SvgLoaderHacker {
  /**
   * Enables the ability to have more than one color patcher.
   */
  fun setSVGColorPatcher(dokiTheme: DokiTheme) {
    if (PluginService.areIconsInstalled()) {
      return
    }

    collectOtherPatcher()
      .ifPresent { otherPatcher ->
        ColorPatcher.setOtherPatcher(otherPatcher)
      }

    ColorPatcher.setDokiTheme(dokiTheme)

    runSafely({
      setHackedPatcher()
    }) {
      println(it)
    }
  }

  private fun setHackedPatcher() {
    val handler = object : InvocationHandler {
      val methods = ColorPatcher.javaClass.methods.map { it.name to it }.toMap()
      override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        if (method == null) return null
        val firstOrNull = methods[method.name]
        val args2 = args ?: emptyArray()
        return firstOrNull?.invoke(
          ColorPatcher, *args2
        )
      }
    }
    val forName = Class.forName("com.intellij.util.SVGLoader\$SvgElementColorPatcherProvider")
    val proxyClass = Proxy.newProxyInstance(
      forName.classLoader,
      arrayOf(forName),
      handler
    )
    val svgLoader = Class.forName("com.intellij.util.SVGLoader")
    val setPatcher = svgLoader.declaredMethods.firstOrNull { it.name == "setColorPatcherProvider" }
    setPatcher?.invoke(null, proxyClass)

    val clazz = Class.forName("com.intellij.ui.svg.SvgKt")
    val setPatcherProvdier = clazz.declaredMethods.firstOrNull { it.name == "setSelectionColorPatcherProvider" }
    setPatcherProvdier?.invoke(null, proxyClass)
  }

  private fun collectOtherPatcher(): Optional<SvgElementColorPatcherProvider> =
    Optional.ofNullable(
      SVGLoader::class.java.declaredFields
        .firstOrNull { it.name == "colorPatcherProvider" },
    )
      .map { ourColorPatcherField ->
        ourColorPatcherField.isAccessible = true
        ourColorPatcherField.get(null)
      }
      .filter { it is SvgElementColorPatcherProvider }
      .filter { it !is ColorPatcher }
      .map {
        val otherPatcher = it as SvgElementColorPatcherProvider
        otherPatcher
      }
}


