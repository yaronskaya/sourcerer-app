// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app

import app.utils.Options
import app.utils.PasswordValidator
import app.utils.UsernameValidator
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration
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

    // Options levels are presented in priority decreasing order.
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

    // Working directory path.
    val localDir = try {
        System.getProperty("user.dir")
    }
    catch (e: SecurityException) { null }

    // User directory path.
    val userDir = try {
        System.getProperty("user.home")
    }
    catch (e: SecurityException) { null }

    init {
        loadLocalLevelConfig()
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

            val config = PropertiesConfiguration(file)

            // Accessing configuration properties. Unknown values should be
            // null to distinguish them from specified values from other levels.
            options.username = config.getString(configUsername, null)
            options.password = config.getString(configPassword, null)
            options.silent = config.getBoolean(configSilent, null)
        }
        catch (e: ConfigurationException) {
            // Error while loading the properties file.
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

            val config = PropertiesConfiguration(file)

            if (options.username != null) {
                config.setProperty(configUsername, options.username)
            }
            if (options.password != null) {
                config.setProperty(configPassword, options.password)
            }
            if (options.silent != null) {
                config.setProperty(configSilent, options.silent)
            }

            config.save(file)

            return true
        }
        catch (e: IOException) {
            // IO error occurred.
        }
        catch (e: SecurityException) {
            // Read access denied.
        }
        catch (e: ConfigurationException) {
            // Error while loading or saving the properties file.
        }

        return false
    }

    fun setCurrentOptions(options: Options) {
        current = options
    }

    fun loadLocalLevelConfig() {
        if (localDir != null) {
            local = loadConfig(localDir)
        }
    }

    fun loadUserLevelConfig() {
        if (userDir != null) {
            user = loadConfig(userDir)
        }
    }

    fun saveLocalLevelConfig(options: Options): Boolean {
        if (userDir != null) {
            return saveConfig(userDir, options)
        }

        return false
    }

    fun saveUserLevelConfig(options: Options): Boolean {
        if (userDir != null) {
            return saveConfig(userDir, options)
        }

        return false
    }

    fun createOptions(pair: List<String>): Options {
        val options = Options()

        if (pair.count() != 2) {
            return options
        }

        val (key, value) = pair

        when (key) {
            configUsername -> {
                if (UsernameValidator().isValidUsername(value)) {
                    options.username = value
                }
            }
            configPassword -> {
                if (PasswordValidator().isValidPassword(value)) {
                    options.password = value
                }
            }
            configSilent -> {
                options.silent = value.toBoolean()
            }
        }

        return options
    }
}
