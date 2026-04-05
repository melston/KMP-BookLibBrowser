package org.elsoft.bkdb.utils

import com.russhwolf.settings.Settings

// This will be our global access point
lateinit var appSettings: Settings

// We still keep an expect function, but we won't call it from common code
// directly if it needs platform-specific initialization.
