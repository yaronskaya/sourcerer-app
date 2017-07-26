// Copyright 2017 Sourcerer Inc. All Rights Reserved.
// Author: Anatoly Kislov (anatoly@sourcerer.io)

package app

import app.model.Commit
import app.model.User
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.result.Result
import java.io.IOException

/**
 * Sourcerer API.
 */
object SourcererApi {
    init {
        FuelManager.instance.basePath = "http://192.168.0.150:3181"
    }

    val username
        get() = Configurator.getUsername()

    val password
        get() = Configurator.getPassword()

    @Throws(IOException::class, HttpException::class)
    fun getUserBlocking(): User {
        val name = "getUserBlocking"
        Logger.debug("Request $name initialized")

        val (request, response, result) = Fuel.get("/user/info")
                .authenticate(username, password)
                .responseString()
        val body = result.get()
        Logger.debug("Request $name success")
        return User().parseFrom(body)
    }

    @Throws(IOException::class, HttpException::class)
    fun postCommitBlocking(commit: Commit): String {
        val name = "postCommitBlocking"
        Logger.debug("Request $name initialized")
        val (request, response, result) = Fuel.get("/commit")
                .authenticate(username, password)
                .body(commit.serialize())
                .responseString()

        val body = result.get()
        Logger.debug("Request $name success")
        return body
    }

    // Temprorary method to send mock data to commit server.
    @Throws(IOException::class, HttpException::class)
    fun postCommitTestBlocking(): String {
        val name = "postCommitTestBlocking"
        Logger.debug("Request $name initialized")
        val (request, response, result) = Fuel.post("/commit")
                .authenticate(username, password)
                .body(CommitProtos.Commit.newBuilder()
                        .setAuthorEmail("author@mail.com")
                        .setAuthorName("author")
                        .setDate(1000000)
                        .setId("hash")
                        .setNumLinesAdd(10)
                        .setNumLinesDeleted(10)
                        .setQommit(false)
                        .setRepoId("hash")
                        .addCommitStats(CommitProtos.Stats.newBuilder()
                                .setLanguage("Java")
                                .setTechnology("Network")
                                .setNumLinesAdd(5)
                                .setNumLinesDeleted(5)
                                .build())
                        .addCommitStats(CommitProtos.Stats.newBuilder()
                                .setLanguage("Python")
                                .setTechnology("OpenCV")
                                .setNumLinesAdd(5)
                                .setNumLinesDeleted(5)
                                .build())
                        .build().toByteArray().toString(Charsets.UTF_8))
                .responseString()

        val body = result.get()
        Logger.debug("Request $name success")
        return body
    }

    fun getUserAsync(success: (String) -> Unit, failure: (String) -> Unit) {
        val name = "getUserAsync"
        Logger.debug("Request $name initialized")
        Fuel.get("/user")
                .authenticate(username, password)
                .responseString { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            Logger.error("Request $name error",
                                    result.getException())
                            failure(result.get())
                        }
                        is Result.Success -> {
                            Logger.debug("Request $name success")
                            Logger.debug(result.get())
                            success(result.get())
                        }
                    }
                }
    }

    fun postCommitAsync(commit: Commit, success: (String) -> Unit,
                   failure: (String) -> Unit) {
        val name = "postCommitAsync"
        Logger.debug("Request $name initialized")
        Fuel.post("/commit")
                .authenticate(username, password)
                .body(commit.serialize())
                .responseString { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            Logger.error("Request $name error",
                                    result.getException())
                            failure(result.get())
                        }
                        is Result.Success -> {
                            Logger.debug("Request $name success")
                            Logger.debug(result.get())
                            success(result.get())
                        }
                    }
                }
    }
}
