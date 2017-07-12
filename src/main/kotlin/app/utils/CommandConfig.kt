// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app.utils

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

@Parameters(separators = "=",
        commandDescription = "Configure Sourcerer app")
class CommandConfig {
    // Key value pair of configurable parameters.
    @Parameter(description = "KEY VALUE", arity = 2, order = 0)
    var pair: List<String> = arrayListOf()

    // Local level config.
    @Parameter(names = arrayOf("--local"),
            description = "Used to specify local level of configuration "
                    +"(default)",

            order = 1)
    var local: Boolean = false

    // User level config.
    @Parameter(names = arrayOf("--user"),
            description = "Used to specify user level of configuration",
            order = 2)
    var user: Boolean = false
}
