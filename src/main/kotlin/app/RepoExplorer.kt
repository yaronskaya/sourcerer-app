// Copyright 2017 Sourcerer Inc. All Rights Reserved.
// Author: Anatoly Kislov (anatoly@sourcerer.io)

package app

import app.utils.CommandExplore
import app.CommitProtos.Commit
import app.utils.CommitMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.Result
import io.reactivex.Observable
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import java.io.IOException

/**
 * RepoExplorer analyzes repositories and uploads stats to server.
 */
class RepoExplorer(options: CommandExplore) {
    val path = options.path
    val isValidPath = try {
        val f = File(path)
        f.isDirectory
    } catch (e: SecurityException) {
        false
    }

    init {
        // FuelManager.instance.basePath = "https://commit.sourcerer.io"
        FuelManager.instance.basePath = "http://localhost:8080"
    }

    fun explore() {
        if (!isValidPath) {
            println("Wrong path to repository.")
        }

        var firstCommit: RevCommit

        // Get commits loading observable and wait for subscribers.
        val observableRevCommits = getObservableRevCommits().publish()

        // Main data flow. Read commits from repo and send them to server.
        observableRevCommits
                .doOnNext {
                    println("Commit: " + it.id.name + " " + it.shortMessage)
                }
                .map{c -> CommitMapper.map(c)}
                .subscribe(
                        {
                            println("onNext")
                            sendCommit(it)
                        },
                        { e ->
                            println("onError")
                            e.printStackTrace()
                        },
                        {
                            println("onComplete")
                        }
                )

        // Additional data flow. Read first commit.
        observableRevCommits
                .take(1)
                .subscribe {
                    firstCommit = it
                }

        // Start emitting commits.
        observableRevCommits.connect()
    }

    fun sendCommit(commit: Commit) {
        val options = Configurator.options
        val username = options.username ?: ""
        val password = options.password ?: ""

        Fuel.post("/commit")
                .authenticate(username, password)
                .body(commit.toByteArray())
                .responseString { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            println("Failure while commit transmission!")
                        }
                        is Result.Success -> {
                            println("Successful commit transmit!")
                        }
                    }
                }
    }

    fun getObservableRevCommits(): Observable<RevCommit> = Observable.create {
        subscriber ->

        val git = try {
            Git.open(File(path))
        } catch (e: IOException) {
            subscriber.onError(e)
            null
        }

        if (git != null) {
            val repository = git.repository
            try {
                val revWalk = RevWalk(repository)
                val commitId = repository.resolve("refs/heads/master")
                revWalk.markStart(revWalk.parseCommit(commitId))
                for (commit in revWalk) {
                    subscriber.onNext(commit)
                }
            } catch (e: Exception) {
                subscriber.onError(e)
            }

            git.close()
        }

        subscriber.onComplete()
    }
}
