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
fun classpathPainterResource(resourcePath: String): Painter {
    val contextClassLoader = remember { Thread.currentThread().contextClassLoader }
    val density = LocalDensity.current
    val resourceBytes =
        remember(resourcePath) {
            checkNotNull(contextClassLoader.getResourceAsStream(resourcePath)) {
                "Resource file not found: $resourcePath"
            }.readBytes()
        }

    val bitmap = remember(resourcePath) { runCatching { resourceBytes.decodeToImageBitmap() }.getOrNull() }
    val vector =
        remember(resourcePath, density) { runCatching { resourceBytes.decodeToImageVector(density) }.getOrNull() }

    return bitmap?.let { BitmapPainter(it) }
        ?: vector?.let { rememberVectorPainter(it) }
        ?: remember(resourcePath, density) {
            runCatching { resourceBytes.decodeToSvgPainter(density) }.getOrElse {
                error("Cannot decode a byte array as ImageBitmap, ImageVector or SVG XML")
            }
        }
}
