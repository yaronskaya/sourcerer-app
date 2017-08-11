// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app

import org.eclipse.jgit.blame.BlameGenerator
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.treewalk.filter.*
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream

import java.util.Date

/**
 * Used to store and identify a code line information in a repo.
 */
class CodeLine(val rev: String, val file: String, val line: Int,
               val authorName: String, val authorEmail: String,
               val age: Int) {
}

/**
 * Used to compute age of code lines in the repo.
 */
class CodeLongevity(repo_path: String) {
    var repo_path: String = repo_path

    var list: MutableList<CodeLine> = mutableListOf()

    init {
        println("READYY>>>>>>>>>>");
        println("Repo: $repo_path")

        val repo = FileRepository(repo_path)
        val revWalk = RevWalk(repo);
        val head = revWalk.parseCommit(repo.readOrigHead()); // master

        computeExistingLines(repo, head)
        computeDeletedLines(repo, revWalk, head)
    }

    fun test(email: String) {
        var sum: Long = 0;
        for (line in list) {
            if (line.authorEmail != email) {
                continue
            }
            sum += line.age
        }
        if (list.size > 0) {
            println("avg code line age for <$email> is ${sum / list.size} seconds")
        }
    }

    /**
     * Computes age of all existing lines in all files of the repo.
     */
    private fun computeExistingLines(repo: FileRepository, head: RevCommit) {
        val treeWalk = TreeWalk(repo);
        treeWalk.setRecursive(true);
        treeWalk.addTree(head.getTree());
        while (treeWalk.next()) {
            // todo: skip binary files.

            val file = treeWalk.getPathString();
            //println("found: ${file}");
            val blameGen = BlameGenerator(repo, file)
            blameGen.push(null, head)
            val blame = blameGen.computeBlameResult()
            val lines = blame.getResultContents()
            var index = 0;
            while (index < lines.size()) {
                val blameCommit = blame.getSourceCommit(index)
                val timeDiff = (head.getCommitTime() - blameCommit.getCommitTime())
                val author = blame.getSourceAuthor(index)
                val authorName = author.getName()
                val authorEmail = author.getEmailAddress()

                list.add(CodeLine(blameCommit.getName(),
                                  file, index,
                                  authorName, authorEmail, timeDiff))
                index++
            }
        }
    }

    /**
     * Computes age of all deleted lines in the repo.
     */
    private fun computeDeletedLines(repo: FileRepository,
                                    revWalk: RevWalk, head: RevCommit) {
        revWalk.markStart(head);

        var childCommit = head;
        for (commit in revWalk) {
            //println("\nparent $commit")
            //println("this $childCommit")

            val df = DiffFormatter(DisabledOutputStream.INSTANCE)
            df.setRepository(repo)
            df.setDetectRenames(true)
            val diffs = df.scan(commit, childCommit);
            //println("filesChanged: ${diffs.size}")

            for (diff in diffs) {
                // todo: skip binary files.

                // A new file, no deleted lines.
                if (diff.changeType == DiffEntry.ChangeType.ADD) {
                    continue
                }

                val file = diff.getOldPath()
                //println("old file: $file, new file: ${diff.getNewPath()}")

                var blame: BlameResult? = null
                //var lines: RawText? = null
                var delta = 0

                for (edit in df.toFileHeader(diff).toEditList()) {
                    val delStart = edit.getBeginA()
                    val delEnd = edit.getEndA()
                    val delCount = edit.getLengthA()
                    val insCount = edit.getLengthB()

                    // Delete case.
                    if (delCount > 0) {
                        if (blame == null) {
                            var blameGen = BlameGenerator(repo, file)
                            blameGen.push(null, commit)
                            blame = blameGen.computeBlameResult()
                            // If no blame, skip it. It may happen, for example,
                            // when a change is an empty folder removal.
                            if (blame == null) {
                                break
                            }
                            //lines = blame.getResultContents()
                        }

                        var index = delStart + delta
                        //println("original file index: $index")

                        while (index < delEnd) {
                            val blameCommit = blame.getSourceCommit(index)
                            //val deletedLine = lines!!.getString(index)
                            //val blameDate = Date(blameCommit.getCommitTime().toLong() * 1000)
                            val timeDiff = (childCommit.getCommitTime() - blameCommit.getCommitTime())
                            val author = blame.getSourceAuthor(index)
                            val authorName = author.getName()
                            val authorEmail = author.getEmailAddress()

                            list.add(CodeLine(blameCommit.getName(),
                                     file, index,
                                     authorName, authorEmail, timeDiff))
                            index++
                        }
                    }

                    delta += insCount - delCount
                }
            }

            childCommit = commit
        }
    }
}
