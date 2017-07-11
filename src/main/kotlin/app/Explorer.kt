// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app

import app.utils.CommandExplore
import java.io.File

/**
 * Explorer is a class that does repository analysis and sends statistics data
 * to Sourcerer.
 */
class Explorer(options: CommandExplore) {
    val path = options.path

    fun explore() {
        var isValidPath = false

        try {
            val f = File(path)
            isValidPath = f.isDirectory
        } catch (e: SecurityException) {
        }

        println(isValidPath)

        //TODO(anatoly): Implement repository analysis.
        //TODO(anatoly): Implement data transfer.
    }
}
