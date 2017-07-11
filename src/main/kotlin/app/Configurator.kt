// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app

import app.utils.Options

/**
 * Configurator is a singleton class that manages configuration files and
 * values of non-command specific options.
 */
object Configurator {
    init {
        val dir = try {
            System.getProperty("user.dir")
        }
        catch (e: SecurityException) { null }

        val home = try {
            System.getProperty("user.dir")
        }
        catch (e: SecurityException) { null }
    }

    fun setCurrentOptions(options: Options) {
        // TODO(anatoly): Implement.
    }
}
