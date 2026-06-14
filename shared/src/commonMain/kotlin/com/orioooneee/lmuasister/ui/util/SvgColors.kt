package com.orioooneee.lmuasister.ui.util

/**
 * Inline ``<style> .cls{prop:val}`` CSS rules onto matching elements as presentation
 * attributes (`fill="…"` etc.). Illustrator exports colour via class-based stylesheets,
 * but Coil 3's SVG renderer ignores `<style>` blocks, so those shapes fall back to solid
 * black. Rewriting the classes as attributes makes the logo render in its real colours.
 */
fun inlineSvgCss(svg: String): String {
    val rules = HashMap<String, String>()  // class name → " a=\"x\" b=\"y\"" attribute string
    val ruleRe = Regex("\\.([A-Za-z0-9_-]+)\\s*\\{([^}]*)\\}")
    Regex("<style[^>]*>(.*?)</style>", RegexOption.DOT_MATCHES_ALL).findAll(svg).forEach { block ->
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
