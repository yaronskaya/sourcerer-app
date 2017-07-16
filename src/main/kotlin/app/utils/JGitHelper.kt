package app.utils

import java.io.File
import java.io.IOException

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder


object JGitHelper {

    @Throws(IOException::class)
    fun openJGitCookbookRepository(): Repository {
        val builder = FileRepositoryBuilder()
        return builder
                .setGitDir(File("/Users/anatoly/IdeaProjects/pullrequesttest"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()
    }

    @Throws(IOException::class)
    fun createNewRepository(): Repository {
        // prepare a new folder
        val localPath = File.createTempFile("TestGitRepository", "")
        if (!localPath.delete()) {
            throw IOException("Could not delete temporary file " + localPath)
        }

        // create the directory
        val repository = FileRepositoryBuilder.create(File(localPath, ".git"))
        repository.create()

        return repository
    }
}