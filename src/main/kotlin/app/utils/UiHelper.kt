// Copyright 2017 Sourcerer Inc. All Rights Reserved.
// Author: Anatoly Kislov (anatoly@sourcerer.io)

package app.utils

object UiHelper {
    fun confirm(message: String, defaultIsYes: Boolean): Boolean {
        val yes = if (defaultIsYes) "Y" else "y"
        val no = if (!defaultIsYes) "N" else "n"
        println("$message [$yes/$no]")
        val oppositeDefaultValue = if (defaultIsYes) no else yes
        if ((readLine() ?: "").toLowerCase() == oppositeDefaultValue) {
            return !defaultIsYes
        }
        return defaultIsYes
    }
}
