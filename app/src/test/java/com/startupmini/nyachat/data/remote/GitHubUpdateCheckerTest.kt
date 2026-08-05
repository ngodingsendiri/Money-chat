package com.startupmini.nyachat.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateCheckerTest {

    @Test
    fun `versi sama tidak lebih baru`() {
        assertFalse(GitHubUpdateChecker.isNewer("1.2.3", "1.2.3"))
    }

    @Test
    fun `versi remote lebih tinggi - patch`() {
        assertTrue(GitHubUpdateChecker.isNewer("1.2.4", "1.2.3"))
    }

    @Test
    fun `versi remote lebih tinggi - minor`() {
        assertTrue(GitHubUpdateChecker.isNewer("1.10.0", "1.2.3"))
    }

    @Test
    fun `versi remote lebih tinggi - major`() {
        assertTrue(GitHubUpdateChecker.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun `versi remote lebih rendah tidak lebih baru`() {
        assertFalse(GitHubUpdateChecker.isNewer("1.2.3", "2.0.0"))
    }

    @Test
    fun `versi dengan prefiks v dibandingkan`() {
        assertTrue(GitHubUpdateChecker.isNewer("v2.0.0", "1.9.0"))
    }

    @Test
    fun `versi dengan prefiks r (skema rilis Nyachat) dibandingkan`() {
        assertTrue(GitHubUpdateChecker.isNewer("r1.0.1", "1.0.0"))
        assertFalse(GitHubUpdateChecker.isNewer("r1.0.0", "1.0.0"))
        assertTrue(GitHubUpdateChecker.isNewer("r2.0.0", "r1.9.9"))
    }

    @Test
    fun `versi tanpa patch - cukup 2 komponen`() {
        assertTrue(GitHubUpdateChecker.isNewer("1.3", "1.2.9"))
    }

    @Test
    fun `versi tidak valid dianggap tidak lebih baru`() {
        assertFalse(GitHubUpdateChecker.isNewer("abc", "1.0.0"))
        assertFalse(GitHubUpdateChecker.isNewer("1.0.0", "bukan-version"))
        assertFalse(GitHubUpdateChecker.isNewer("", "1.0.0"))
    }
}
