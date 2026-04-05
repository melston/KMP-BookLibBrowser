package org.elsoft.bkdb.utils

import com.russhwolf.settings.Settings
import java.util.Properties

object ConfigManager {
    private val settings: Settings = appSettings

    var viewerCommand: String
        get() = settings.getString("viewer_command", "") // "evince" is the default
        set(value) = settings.putString("viewer_command", value)

    var dbPassword: String
        get() = settings.getString("db.password", "")
        set(value) = settings.putString("db.password", value)

    var dbUrl: String
        get() = settings.getString("db.url", "")
        set(value) = settings.putString("db.url", value)

    var dbUser: String
        get() = settings.getString("db.user", "")
        set(value) = settings.putString("db.user", value)

    var dropboxAppKey: String
        get() = settings.getString("dropbox.app_key", "")
        set(value) = settings.putString("dropbox.app_key", value)

    var dropboxAppSecret: String
        get() = settings.getString("dropbox.app_secret", "")
        set(value) = settings.putString("dropbox.app_secret", value)

    var dropboxRefreshToken: String
        get() = settings.getString("dropbox.refresh_token", "")
        set(value) = settings.putString("dropbox.refresh_token", value)

    var dropboxRoot: String
        get() = settings.getString("dropbox.root", "")
        set(value) = settings.putString("dropbox.root", value)

    fun isConfigured(): Boolean {
        // If the migration worked, these won't be empty anymore!
        return dbUrl.isNotEmpty()
    }

    fun initializeFromProperties(props: Properties) {
        props.getProperty("viewer_command")?.let { viewerCommand = it }
        props.getProperty("db.password")?.let { dbPassword = it }
        props.getProperty("db.url")?.let { dbUrl = it }
        props.getProperty("db.user")?.let { dbUser = it }
        props.getProperty("dropbox.app_key")?.let { dropboxAppKey = it }
        props.getProperty("dropbox.app_secret")?.let { dropboxAppSecret = it }
        props.getProperty("dropbox.refresh_token")?.let { dropboxRefreshToken = it }
        props.getProperty("dropbox.root")?.let { dropboxRoot = it }
    }
}