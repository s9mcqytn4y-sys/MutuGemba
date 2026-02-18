package id.co.nierstyd.mutugemba.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.resources.classpathPainterResource
import java.nio.file.Files
import java.nio.file.Paths

fun main() =
    application {
        configurePdfBoxRuntime()
        Window(
            onCloseRequest = ::exitApplication,
            title = AppStrings.App.WindowTitle,
            icon = classpathPainterResource("branding/app_icon_512.png"),
        ) {
            MutuGembaApp()
        }
    }

private fun configurePdfBoxRuntime() {
    // Keep PDFBox font cache in app-local folder to avoid repeated full rebuild on each run.
    runCatching {
        val cacheDir =
            Paths
                .get(System.getProperty("user.home"), ".mutugemba", "cache", "pdfbox")
                .also { Files.createDirectories(it) }
        System.setProperty("pdfbox.fontcache", cacheDir.toString())
    }

    // Route noisy commons-logging categories to error level for cleaner operator logs.
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog")
    System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "warn")
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.fontbox.ttf", "error")
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.fontbox", "error")
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.pdfbox", "error")
    System.setProperty(
        "org.apache.commons.logging.simplelog.log.org.apache.pdfbox.pdmodel.font.FileSystemFontProvider",
        "error",
    )
    System.setProperty(
        "org.apache.commons.logging.simplelog.log.org.apache.fontbox.ttf.CmapSubtable",
        "error",
    )
    System.setProperty(
        "org.apache.commons.logging.simplelog.log.org.apache.fontbox.ttf.TTFParser",
        "error",
    )
    System.setProperty(
        "org.apache.commons.logging.simplelog.log.org.apache.pdfbox.pdmodel.PDDocument",
        "error",
    )
}
