// Copyright 2017 Sourcerer Inc. All Rights Reserved.

package app

import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectLoader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream

import java.util.Date

/**
 * Identifies a code line in a file in a revision.
 */
class RevCommitLine(val commit: RevCommit, val file: String, val line: Int)

/**
 * Represents a code line.
 *
 * TODO(Alex): the text arg is solely for testing proposes (remove it)
 */
class CodeLine(val from: RevCommitLine, val to: RevCommitLine, val text: String) {

    // XXX(Alex): oldId and newId may be computed as a hash built from commit,
    // file name and line number, if we are going to send the data outside a
    // local machine.

    /**
     * Id of the code line in a revision when the line was added. Used to
     * update the line's lifetime computed in previous iterations.
     * 
     */
    val oldId: String = ""

    /**
     * Id of the line in head revision (if this is an existing line) or
     * a revision where the line was deleted.
     */
    val newId: String = ""

    /**
     * A line age in seconds.
     */
    val age = to.commit.getCommitTime() - from.commit.getCommitTime()

    /**
     * A pretty print of a code line; debugging.
     */
    fun printme() {
        val fd = Date(from.commit.getCommitTime().toLong() * 1000).toLocaleString()
        val fc = "${from.commit.getName()} '${from.commit.getShortMessage()}'"
        val tc = "${to.commit.getName()} '${to.commit.getShortMessage()}'"
        val td = Date(to.commit.getCommitTime().toLong() * 1000).toLocaleString()
        println("Line '$text' - '${from.file}:${from.line}' added in $fc $fd")
        println("  last known as '${to.file}:${to.line}' in $tc $td")
    }
}

/**
 * Used to compute age of code lines in the repo.
 */
class CodeLongevity(repoPath: String, tailRev: String) {
    val repo = FileRepository(repoPath)
    val head: RevCommit = RevWalk(repo).parseCommit(repo.resolve("HEAD"));
    val tail: RevCommit? =
        if (tailRev != "") RevWalk(repo).parseCommit(repo.resolve(tailRev))
        else null

    var codeLines: MutableList<CodeLine> = mutableListOf()

    init {
        compute()
    }

    fun test(email: String) {
        var sum: Long = 0
        var total: Long = 0;
        for (line in codeLines) {
            val author = line.from.commit.getAuthorIdent()
            if (author.getEmailAddress() != email) {
                continue
            }
            //println("got a line, age ${line.age}\n ${line.printme()}")
            sum += line.age
            total++
        }

        //println("All lines:")
        //codeLines.forEach { line -> line.printme() }

        var avg = if (total > 0) sum / total else 0
        println("avg code line age for <$email> is ${avg} seconds")
    }

    /**
     * Computes age of all lines in the repo through all revisions.
     */
    private fun compute() {
        val treeWalk = TreeWalk(repo)
        treeWalk.setRecursive(true)
        treeWalk.addTree(head!!.getTree())

        val files: MutableMap<String, ArrayList<RevCommitLine>> = mutableMapOf()

        // Build a map of file names and their code lines.
        while (treeWalk.next()) {
            // TODO(alex): skip binary files.
            val fileName = treeWalk.getPathString()
            val fileLoader = repo.open(treeWalk.getObjectId(0))
            val fileText = RawText(fileLoader.getBytes())
            val linesNum = fileText.size()
            var lines = ArrayList<RevCommitLine>(linesNum)
            var idx = 0
            while (idx < fileText.size()) {
                lines.add(RevCommitLine(head, fileName, idx))
                idx++
            }
            files.put(fileName, lines)
        }
  
        val df = DiffFormatter(DisabledOutputStream.INSTANCE)
        df.setRepository(repo)
        df.setDetectRenames(true)

        val revWalk = RevWalk(repo)
        revWalk.markStart(head)

        var commit: RevCommit? = head
        var parentCommit: RevCommit? = revWalk.next()  // proceed to head
        do {
            parentCommit = revWalk.next()

            println("commit: ${commit!!.getName()}; '${commit.getShortMessage()}'")
            if (parentCommit != null) {
                println("parent commit: ${parentCommit.getName()}; '${parentCommit.getShortMessage()}'")
            }
            else {
                println("parent commit: null")
            }

            val diffs = df.scan(parentCommit, commit)

            for (diff in diffs) {
                // XXX(alex): does it happen in the wilds?
                if (diff.changeType == DiffEntry.ChangeType.COPY) {
                    continue
                }

                // File was renamed, tweak the files map.
                if (diff.changeType == DiffEntry.ChangeType.RENAME) {
                    println("old path: ${diff.getOldPath()}")
                    println("new path: ${diff.getNewPath()}")
                    files.set(diff.getOldPath(),
                              files.remove(diff.getNewPath())!!)
                    println("oopsy")
                    //continue
                }

                // Get a edit list and traverse it backwards to avoid indices
                // adjustment.
                val editList = df.toFileHeader(diff).toEditList().asReversed()
                for (edit in editList) {
                    val delStart = edit.getBeginA()
                    val delEnd = edit.getEndA()
                    val delCount = edit.getLengthA()
                    var insStart = edit.getBeginB()
                    var insEnd = edit.getEndB()
                    val insCount = edit.getLengthB()
                    println("del ($delStart, $delEnd), ins ($insStart, $insEnd)");

                    // Deletion case. Chaise down the deleted lines through the
                    // history.
                    if (delCount > 0) {
                        val oldPath = diff.getOldPath()
                        var tmpLines = ArrayList<RevCommitLine>(delCount)
                        var idx = delStart;
                        while (idx < delEnd) {
                            tmpLines.add(RevCommitLine(commit, oldPath, idx));
                            idx++
                        }

                        files.get(oldPath)!!.addAll(delCount, tmpLines)
                    }

                    // Insertion case. Report it.
                    if (insCount > 0) {
                        val newPath = diff.getNewPath()
                        var newLines = files.get(newPath)!!
                        val fileLoader = repo.open(diff.getNewId().toObjectId())
                        val fileText = RawText(fileLoader.getBytes())

                        var idx = insStart
                        while (idx < insEnd) {
                            val from = RevCommitLine(commit, newPath, idx)
                            var to = newLines.get(idx)
                            val cl = CodeLine(from, to, fileText.getString(idx))
                            codeLines.add(cl)
                            idx++
                        }
                        newLines.subList(insStart, insEnd).clear()
                    }
                }
            }
            commit = parentCommit
        }
        while (parentCommit != null && parentCommit != tail)

        // If tail revision is specified then we have file code lines unclaimed,
        // push all of them into result lines list.
        if (tail != null) {
            for ((file, lines) in files) {
                println(file)
                println(lines.size)
                var idx = 0
                while (idx < lines.size) {
                    val from = RevCommitLine(tail, file, idx)
                    val cl = CodeLine(from, lines[idx], "no data")
                    codeLines.add(cl)
                    idx++
                }
            }
        }
    }
}
