// Copyright 2017 Sourcerer Inc. All Rights Reserved.
// Author: Anatoly Kislov (anatoly@sourcerer.io)

package app.utils

import app.CommitProtos.Commit
import org.eclipse.jgit.revwalk.RevCommit

/**
 * Maps RevCommit to Commit message.
 */
object CommitMapper {
    fun map(rev: RevCommit): Commit {
        return Commit.newBuilder()
                .setId(rev.id.name)  // TODO(anatoly): Use SHA-256.
                .setRepoId(0)  // TODO(anatoly).
                .setAuthorName(rev.authorIdent.name)
                .setAuthorEmail(rev.authorIdent.emailAddress)
                .setDate(rev.commitTime)
                .setQommit(false)  // TODO(anatoly).
                .setNumLinesAdd(0)  // TODO(anatoly).
                .setNumLinesDeleted(0)  // TODO(anatoly).
                .build()
    }
}
