// Copyright 2017 Sourcerer Inc. All Rights Reserved.
// Author: Anatoly Kislov (anatoly@sourcerer.io)

package app

import io.sentry.Sentry
import io.sentry.context.Context
import io.sentry.event.Breadcrumb
import io.sentry.event.UserBuilder
import io.sentry.event.BreadcrumbBuilder
import java.util.*


/**
 * Singleton class that logs events of different levels.
 */
object Logger {
    object Events {
        val START = "start"
        val AUTH = "auth"
        val CONFIG_SETUP = "config/setup"
        val CONFIG_CHANGED = "config/changed"
        val HASHING_REPO_SUCCESS = "hashing/repo/success"
        val HASHING_SUCCESS = "hashing/success"
        val EXIT = "exit"
    }

    /**
     * Current log level. All that higher than this level will not be displayed.
     */
    private const val LEVEL = BuildConfig.LOG_LEVEL

    /**
     * Error level.
     */
    private const val ERROR = 0

    /**
     * Warning level.
     */
    private const val WARN = 1

    /**
     * Information level.
     */
    private const val INFO = 2

    /**
     * Debug level.
     */
    private const val DEBUG = 3

    /**
     * Trace level. For extremely detailed and high volume debug logs.
     */
    private const val TRACE = 4

    /**
     * Print stack trace on error log.
     */
    private const val PRINT_STACK_TRACE = BuildConfig.PRINT_STACK_TRACE

    /**
     * Context of Sentry error reporting software for adding info.
     */
    private val sentryContext: Context

    /**
     * Username used for error reporting.
     */
    var username: String? = null
        set(value) {
            sentryContext.user = UserBuilder().setUsername(value).build()
            Analytics.username = value ?: ""
        }

    var uuid: String? = null
        set(value) {
            Analytics.uuid = value ?: ""
        }

    init {
        Sentry.init(BuildConfig.SENTRY_DSN)
        sentryContext = Sentry.getContext()
        addTags()
    }

    /**
     * Log error message with exception info.
     * Don't log private information with this method.
     *
     * @property e the exception if presented.
     * @property message the message for user and logs.
     * @property logOnly only log to console, no additional actions.
     */
    fun error(e: Throwable, message: String = "", logOnly: Boolean = false) {
        val finalMessage = if (message.isNotBlank()) { message + ": " }
        else { "" } + e.message
        if (LEVEL >= ERROR) {
            println("[e] $finalMessage")
            if (PRINT_STACK_TRACE) {
                e.printStackTrace()
            }
        }
        if (!logOnly) {
            Analytics.trackError(e)
            Sentry.capture(e)
        }
        addBreadcrumb(finalMessage, Breadcrumb.Level.ERROR)
    }

    /**
     * Log warning message. Don't log private information with this method.
     */
    fun warn(message: String) {
        if (LEVEL >= WARN) {
            println("[w] $message.")
        }
        addBreadcrumb(message, Breadcrumb.Level.WARNING)
    }

    /**
     * Log information message. Don't log private information with this method.
     */
    fun info(message: String, event: String = "") {
        if (LEVEL >= INFO) {
            println("[i] $message.")
        }
        if (event.isNotBlank()) {
            Analytics.trackEvent(event)
        }
        addBreadcrumb(message, Breadcrumb.Level.INFO)
    }

    /**
     * Log debug message.
     */
    fun debug(message: String) {
        if (LEVEL >= DEBUG) {
            println("[d] $message.")
        }
    }

    /**
     * Log trace message.
     */
    fun trace(message: String) {
        if (LEVEL >= TRACE) {
            println("[t] $message.")
        }
    }

    private fun addBreadcrumb(message: String, level: Breadcrumb.Level) {
        sentryContext.recordBreadcrumb(BreadcrumbBuilder()
            .setMessage(message)
            .setLevel(level)
            .setTimestamp(Date())
            .build())
    }

    private fun addTags() {
        val default = "unavailable"
        val osName = System.getProperty("os.name", default)
        val osVersion = System.getProperty("os.version", default)
        val javaVendor = System.getProperty("java.vendor", default)
        val javaVersion = System.getProperty("java.version", default)

        sentryContext.addTag("environment", BuildConfig.ENVIRONMENT)
        sentryContext.addTag("version", BuildConfig.VERSION)
        sentryContext.addTag("version-code", BuildConfig.VERSION_CODE
                                                        .toString())
        sentryContext.addTag("os-name", osName)
        sentryContext.addTag("os-version", osVersion)
        sentryContext.addTag("java-vendor", javaVendor)
        sentryContext.addTag("java-version", javaVersion)
    }
}
