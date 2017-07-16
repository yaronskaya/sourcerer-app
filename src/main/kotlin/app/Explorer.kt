// Copyright 2017 Sourcerer Inc. All Rights Reserved.
// Author: Anatoly Kislov (anatoly@sourcerer.io)

package app

import app.utils.CommandExplore
import app.CommitProtos.Commit
import app.CommitProtos.Stats
import app.utils.JGitHelper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
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

        FuelManager.instance.basePath = "https://requestb.in/12whl0a1"

        val stats = arrayListOf(Stats.newBuilder()
                .setLanguage("C#")
                .setNumLinesAdd(10)
                .build(), Stats.newBuilder()
                .setLanguage("Java")
                .setNumLinesAdd(15)
                .build(), Stats.newBuilder()
                .setLanguage("Kotlin")
                .setNumLinesAdd(2)
                .build())

        /*sendCommit(Commit.newBuilder()
                .setAuthorEmail("hello@world.com")
                .setAuthorName("helly")
                .setDate("2016-07-17")
                .setQommit(true)
                .setRepoId(1)
                .setId(10)
                .addAllCommitStats(stats)
                .build())*/

        iterateCommits()

        //TODO(anatoly): Implement repository analysis.
        //TODO(anatoly): Implement data transfer.
    }
}

fun sendCommit(commit: Commit) {
    val options = Configurator.options
    val username = options.username ?: ""
    val password = options.password ?: ""

    Fuel.post("/commit")
            .authenticate(username, password)
            .body(commit.toByteArray())
            .responseString { req, res, result ->
        when (result) {
            is Result.Failure -> {
                println(req)
                println(res)
                println("Failure")
            }
            is Result.Success -> {
                println(req)
                println(res)
                println("Success")
                println(result)
            }
        }
    }
}

fun getInfo(commit: Commit) {
    Fuel.get("/info").responseString { _, _, result ->
        when (result) {
            is Result.Failure -> {
                println("Failure")
            }
            is Result.Success -> {
                println("Success")
                println(result)
            }
        }
    }
}

fun iterateCommits() {
    try {
        val repository = JGitHelper.openJGitCookbookRepository()
        val head = repository.exactRef("refs/heads/master")

        // a RevWalk allows to walk over commits based on some filtering that is defined
        try {
            println("test1")
            val walk = RevWalk(repository)
            val commit = walk.parseCommit(head.getObjectId())
            println("Start-Commit: " + commit)

            println("Walking all commits starting at HEAD")
            walk.markStart(commit)
            var count = 0
            for (rev in walk) {
                println("Commit: " + rev)
                count++
            }
            println(count)

            walk.dispose()
        }
        catch (e: Exception) {}
    }
    catch (e: Exception) {}
}
