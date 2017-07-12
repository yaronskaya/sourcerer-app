// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app

import app.utils.Options
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.apache.commons.configuration2.ex.ConfigurationException
import java.io.File
import java.io.IOException

/**
 * Configurator is a singleton class that manages configuration files and
 * values of non-command specific options.
 */
object Configurator {
    val configFileName = "sourcerer.properties"
    val configUsername = "username"
    val configPassword = "password"
    val configSilent = "silent"

    private var current: Options = Options()  // Command-line arguments.
    private var local: Options = Options()  // Local repository config file.
    private var user: Options = Options()  // Global user defined config file.
    private val default: Options  // Default values.
        get() {
            val default = Options()
            default.silent = false
            return default
        }

    val options: Options  // Final options that will be used by app.
        get() = mergeLevels()

    init {
        // User config location is known so load it at initialization.
        loadUserLevelConfig()
    }

    // Merges different levels of options into one options object in the next
    // order: current, local, user, default.
    private fun mergeLevels(): Options {
        val levels = arrayListOf(current, local, user, default)
        val merged = Options()

        for (level in levels) {
            if (merged.username == null) {
                merged.username = level.username
            }
            if (merged.password == null) {
                merged.password = level.password
            }
            if (merged.silent == null) {
                merged.silent = level.silent
            }
        }

        return merged
    }

    // Loads config file from specified path.
    private fun loadConfig(path: String): Options {
        val options = Options()

        try {
            val file = File(path + File.separator + configFileName)

            if (!file.exists() || !file.isFile) {
                return options  // No configuration file
            }

            val config = Configurations().properties(file)

            // Accessing configuration properties. Unknown values should be
            // null to distinguish them from specified values from other levels.
            options.username = config.getString(configUsername, null)
            options.password = config.getString(configPassword, null)
            options.silent = config.getBoolean(configSilent, null)
        }
        catch (e: ConfigurationException) {
            // Error while initializing a Configuration object.
        }
        catch (e: SecurityException) {
            // Read access denied.
        }

        return options
    }

    // Saves config file to specified path. Returns true on success.
    private fun saveConfig(path: String, options: Options): Boolean {
        try {
            val file = File(path + File.separator + configFileName)

            file.createNewFile()  // Creates a new file if it didn't exist.

            val config = Configurations().properties(file)

            if (options.username != null) {
                config.setProperty(configUsername, options.username)
            }
            if (options.password != null) {
                config.setProperty(configPassword, options.password)
            }
            if (options.silent != null) {
                config.setProperty(configSilent, options.silent)
            }

            return true
        }
        catch (e: IOException) {
            // IO error occurred.
        }
        catch (e: SecurityException) {
            // Read access denied.
        }
        catch (e: ConfigurationException) {
            // Error while initializing a Configuration object.
        }

        return false
    }

    fun setCurrentOptions(options: Options) {
        current = options
    }

    fun loadLocalLevelConfig(path: String) {
        try {
            local = loadConfig(path)
        }
        catch (e: SecurityException) {
            // Read access denied.
        }
    }

    fun loadUserLevelConfig() {
        try {
            // Get user directory path.
            val dir = System.getProperty("user.dir")
            user = loadConfig(dir)
        }
        catch (e: SecurityException) {
            // Read access denied.
        }
    }
}
