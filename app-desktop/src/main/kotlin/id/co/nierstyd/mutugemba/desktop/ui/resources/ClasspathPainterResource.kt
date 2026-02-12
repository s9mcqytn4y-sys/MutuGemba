package id.co.nierstyd.mutugemba.desktop.ui.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.decodeToImageVector
import org.jetbrains.compose.resources.decodeToSvgPainter

@Composable
@Suppress("ReturnCount")
fun classpathPainterResource(resourcePath: String): Painter {
    val contextClassLoader = remember { Thread.currentThread().contextClassLoader }
    val density = LocalDensity.current
    val normalizedPath = remember(resourcePath) { resourcePath.lowercase() }
    val resourceBytes =
        remember(resourcePath) {
            checkNotNull(contextClassLoader.getResourceAsStream(resourcePath)) {
                "Resource file not found: $resourcePath"
            }.readBytes()
        }

    val bitmap =
        remember(resourcePath) {
            if (normalizedPath.endsWith(".xml") || normalizedPath.endsWith(".svg")) {
                null
            } else {
                runCatching { resourceBytes.decodeToImageBitmap() }.getOrNull()
            }
        }

    if (bitmap != null) return BitmapPainter(bitmap)

    if (normalizedPath.endsWith(".xml")) {
        val vector =
            remember(resourcePath, density) {
                runCatching { resourceBytes.decodeToImageVector(density) }.getOrNull()
            }
        checkNotNull(vector) { "Cannot decode resource as vector XML: $resourcePath" }
        return rememberVectorPainter(vector)
    }

    if (normalizedPath.endsWith(".svg")) {
        return remember(resourcePath, density) {
            runCatching { resourceBytes.decodeToSvgPainter(density) }.getOrElse {
                error("Cannot decode resource as SVG: $resourcePath")
            }
        }
    }

    val startsWithXml =
        remember(resourcePath) {
            resourceBytes
                .dropWhile { byte -> byte.toInt().toChar().isWhitespace() }
                .firstOrNull() == '<'.code.toByte()
        }

    if (startsWithXml) {
        val vector =
            remember(resourcePath, density) {
                runCatching { resourceBytes.decodeToImageVector(density) }.getOrNull()
            }
        if (vector != null) return rememberVectorPainter(vector)

        return remember(resourcePath, density) {
            runCatching { resourceBytes.decodeToSvgPainter(density) }.getOrElse {
                error("Cannot decode resource as ImageBitmap, Vector XML, or SVG: $resourcePath")
            }
        }
    }

    error("Cannot decode resource as bitmap: $resourcePath")
}
