package org.elsoft.bkdb.utils

import java.io.File
import java.util.Properties

fun migrateLegacyConfig() {
    val path = PlatformUtils.getLegacyConfigPath()
    val oldFile = File(path)

    if (oldFile.exists()) {
        val props = Properties()
        oldFile.inputStream().use { props.load(it) }

        // Map to ConfigManager
        ConfigManager.initializeFromProperties(props)

        // Rename to .bak
        oldFile.renameTo(File("$path.bak"))
    }
}