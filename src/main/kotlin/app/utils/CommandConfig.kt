// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app.utils

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

@Parameters(separators = "=",
        commandDescription = "Configure Sourcerer app")
class CommandConfig {
    // Key value pair of configurable parameters.
    @Parameter(description = "KEY VALUE")
    var params: List<String> = arrayListOf()

    // User level config.
    @Parameter(names = arrayOf("--user"))
    var user: Boolean = false

    // Local level config.
    @Parameter(names = arrayOf("--local"))
    var local: Boolean = true
}
