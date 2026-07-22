package com.orioooneee.lmuasister.ui.util

/**
 * Patch SVG features that Coil's SVG renderer is weak at for our CDN artwork:
 * class-based CSS colours and simple symbol/use reuse. This keeps track emblems and
 * weather icons rendering as authored instead of black or blank.
 */
fun patchSvgForCoil(svg: String): String =
    inlineSvgUses(inlineSvgCss(svg))

private fun inlineSvgCss(svg: String): String {
    val rules = HashMap<String, String>()
    val ruleRe = Regex("\\.([A-Za-z0-9_-]+)\\s*\\{([^}]*)\\}")
    Regex("<style[^>]*>([\\s\\S]*?)</style>").findAll(svg).forEach { block ->
        ruleRe.findAll(block.groupValues[1]).forEach { rule ->
            val attrs = rule.groupValues[2].split(";").mapNotNull { decl ->
                val kv = decl.split(":", limit = 2)
                if (kv.size == 2 && kv[0].isNotBlank()) " ${kv[0].trim()}=\"${kv[1].trim()}\"" else null
            }.joinToString("")
            if (attrs.isNotEmpty()) rules[rule.groupValues[1]] = attrs
        }
    }
    if (rules.isEmpty()) return svg
    return Regex("class=\"([^\"]*)\"").replace(svg) { m ->
        val add = m.groupValues[1].trim().split(Regex("\\s+")).joinToString("") { rules[it].orEmpty() }
        m.value + add
    }
}

private fun inlineSvgUses(svg: String): String {
    val symbols = Regex("""<symbol\b([^>]*)\bid="([^"]+)"([^>]*)>([\s\S]*?)</symbol>""")
        .findAll(svg)
        .associate { it.groupValues[2] to it.groupValues[4] }
    if (symbols.isEmpty()) return svg

    val useRe = Regex("""<use\b([^>]*)(?:/>|>\s*</use>)""")
    return useRe.replace(svg) { match ->
        val attrs = match.groupValues[1]
        val id = Regex("""(?:xlink:href|href)=["']#([^"']+)["']""").find(attrs)?.groupValues?.getOrNull(1)
        val body = id?.let(symbols::get) ?: return@replace match.value
        val translate = buildList {
            Regex("""\bx=["']([^"']+)["']""").find(attrs)?.groupValues?.getOrNull(1)?.let { x ->
                val y = Regex("""\by=["']([^"']+)["']""").find(attrs)?.groupValues?.getOrNull(1) ?: "0"
                add("translate($x $y)")
            }
            Regex("""\btransform=["']([^"']+)["']""").find(attrs)?.groupValues?.getOrNull(1)?.let(::add)
        }.joinToString(" ")
        if (translate.isBlank()) "<g>$body</g>" else """<g transform="$translate">$body</g>"""
    }
}
