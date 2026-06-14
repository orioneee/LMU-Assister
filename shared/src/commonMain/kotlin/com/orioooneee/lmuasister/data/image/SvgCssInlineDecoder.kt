package com.orioooneee.lmuasister.data.image

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.svg.SvgDecoder
import com.orioooneee.lmuasister.ui.util.inlineSvgCss
import okio.Buffer

/**
 * A Coil [Decoder] that rewrites an SVG's `<style>` CSS classes into presentation
 * attributes before handing it to the real [SvgDecoder]. Illustrator logos colour their
 * shapes via class stylesheets, which Coil's SVG renderer ignores → everything goes black.
 *
 * It sits inside Coil's pipeline, so the network fetch, disk cache and memory cache all
 * work normally (keyed by the request URL); we only transform the bytes at decode time.
 */
class SvgCssInlineDecoder(
    private val result: SourceFetchResult,
    private val options: Options,
    private val imageLoader: ImageLoader,
) : Decoder {

    override suspend fun decode(): DecodeResult? {
        val original = result.source.source().readUtf8()
        val fixed = inlineSvgCss(original)
        val patched = SourceFetchResult(
            source = ImageSource(Buffer().apply { writeUtf8(fixed) }, options.fileSystem),
            mimeType = "image/svg+xml",
            dataSource = result.dataSource,
        )
        return SvgDecoder.Factory().create(patched, options, imageLoader)?.decode()
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (!isSvg(result)) return null
            return SvgCssInlineDecoder(result, options, imageLoader)
        }

        private fun isSvg(result: SourceFetchResult): Boolean {
            if (result.mimeType?.contains("svg", ignoreCase = true) == true) return true
            // Sniff the first bytes for an <svg root (without consuming the real source).
            val head = Buffer().also { result.source.source().peek().read(it, 1024) }.readUtf8()
            return head.contains("<svg", ignoreCase = true)
        }
    }
}
