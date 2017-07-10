// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app

import app.utils.*
import java.io.IOError

class Greeter(args: Args) {
    var username: String? = args.username  // Sourcerer account username.
    var password: String? = args.password  // Sourcerer account password.
    var path: String? = args.path  // Path to analyzed repository.
    var silent: Boolean = args.silent  // Mode without displaying messages.

    fun run() {
        if (!silent) {
            println("Sourcerer app. More info at http://sourcerer.io")
        }

        if (username == null) {
            println("Enter your Sourcerer username:")
            var usernameTemp = readLine()

            while (usernameTemp == null
                    || UsernameValidator.isValidUsername(usernameTemp)) {
                println("Enter correct Sourcerer username "
                        + "(use only latin alphabet, numbers, underscore, "
                        + "plus, minus or dot).")
                usernameTemp = readLine()
            }

            username = usernameTemp
        }

        if (password == null) {
            var passwordTemp: String?
            println("Enter your Sourcerer password:")

            // Check for the console is necessary because when running from IDE
            // console is simulated using in, out and err streams and
            // application aren't associated with any terminal/console.
            val console = System.console()
            if (console != null) {
                try {
                    passwordTemp = System.console().readPassword()
                            .joinToString()
                    while (passwordTemp == null
                            || PasswordValidator
                            .isValidPassword(passwordTemp)) {
                        println("Enter correct Sourcerer password.")
                        passwordTemp = System.console().readPassword()
                                .joinToString()
                    }
                } catch (e: IOError) {
                    passwordTemp = ""
                }
            } else {
                passwordTemp = readLine()

                while (passwordTemp == null
                        || PasswordValidator.isValidPassword(passwordTemp)) {
                    println("Enter correct Sourcerer password.")
                    passwordTemp = readLine()
                }
            }

            password = passwordTemp
        }

        // TODO(anatoly): Implement authorization check.

        if (path == null) {
            println("Enter path to the folder that contains "
                    + "your local repository")
            var pathTemp = readLine()

            while (pathTemp == null
                    || PathValidator.isValidPath(pathTemp)) {
                println("Enter correct path to the folder with repository.")
                pathTemp = readLine()
            }

            path = pathTemp
        }
    }
}
