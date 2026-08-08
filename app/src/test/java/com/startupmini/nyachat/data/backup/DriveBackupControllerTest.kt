package com.startupmini.nyachat.data.backup

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P4.5: alur backup/restore Google Drive diuji dengan DriveBackupApi palsu —
 * tanpa jaringan nyata. Mencakup: backup sukses (upload + prune), konsen OAuth
 * (aksi diulang setelah disetujui), restore lintas-workspace (konfirmasi
 * eksplisit), restore workspace sama (langsung diterapkan), tombol Batal, dan
 * silent backup harian.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DriveBackupControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Fake API Drive — mencatat panggilan & bisa diset untuk menggantung. */
    private class FakeDriveApi : DriveBackupApi {
        var accessTokenResult: BackupResult<String> = BackupResult.Success("token")
        var uploadResult: BackupResult<Unit> = BackupResult.Success(Unit)
        var listResult: BackupResult<List<DriveBackupFile>> = BackupResult.Success(emptyList())
        var downloadResult: BackupResult<String> = BackupResult.Success("""{"app":"Nyachat"}""")
        var uploadCalls = 0
        var pruneCalls = 0
        var lastToken: String? = null
        var lastFileName: String? = null
        var lastJson: String? = null
        /** true → uploadBackup menggantung sampai coroutine dibatalkan (tes Batal). */
        var hangUpload = false

        override suspend fun getAccessToken(context: Context, email: String): BackupResult<String> =
            accessTokenResult

        override suspend fun uploadBackup(
            context: Context,
            token: String,
            fileName: String,
            json: String
        ): BackupResult<Unit> {
            uploadCalls++
            lastToken = token
            lastFileName = fileName
            lastJson = json
            if (hangUpload) awaitCancellation()
            return uploadResult
        }

        override suspend fun listBackups(
            context: Context,
            token: String
        ): BackupResult<List<DriveBackupFile>> = listResult

        override suspend fun downloadBackup(
            context: Context,
            token: String,
            fileId: String
        ): BackupResult<String> = downloadResult

        override suspend fun pruneOldBackups(
            context: Context,
            token: String,
            keep: Int
        ): BackupResult<Unit> {
            pruneCalls++
            return BackupResult.Success(Unit)
        }
    }

    private fun newController(api: FakeDriveApi, scope: kotlinx.coroutines.CoroutineScope): DriveBackupController {
        // UnconfinedTestDispatcher: operasi controller (yang aslinya berjalan di
        // Dispatchers.IO) dieksekusi langsung di scheduler test tanpa kabur ke
        // thread lain — advanceUntilIdle tetap deterministik.
        val c = DriveBackupController(scope, context, api, UnconfinedTestDispatcher())
        c.currentEmail = { "test@example.com" }
        c.getWorkspacePin = { "11111111" }
        c.buildBackupJson = { """{"app":"Nyachat"}""" }
        c.parseRestore = { _, _ -> null }
        c.restoreParsedBackup = { true }
        c.getEncryptionEnabled = { false }
        c.onSuccessfulBackup = { }
        return c
    }

    @Test
    fun backupBerhasilMenguploadDanPrune() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)

        controller.startBackup()
        advanceUntilIdle()

        assertEquals(1, api.uploadCalls)
        assertEquals(1, api.pruneCalls)
        assertEquals("token", api.lastToken)
        assertTrue(api.lastFileName!!.startsWith("Nyachat-backup-"))
        assertFalse(controller.busy.value)
        assertTrue(controller.message.value!!.contains("Backup berhasil"))
    }

    @Test
    fun konsenOAuthMemunculkanIntentDanAksiDiulangSetelahDisetujui() = runTest {
        val api = FakeDriveApi()
        api.accessTokenResult = BackupResult.ConsentRequired(Intent(Intent.ACTION_VIEW))
        val controller = newController(api, this)

        controller.startBackup()
        advanceUntilIdle()

        // Modal konsen muncul, belum ada upload.
        assertNotNull(controller.consentIntent.value)
        assertEquals(0, api.uploadCalls)

        // User menyetujui → aksi (startBackup) diulang otomatis.
        api.accessTokenResult = BackupResult.Success("token")
        controller.onConsentResult(true)
        advanceUntilIdle()

        assertEquals(1, api.uploadCalls)
        assertEquals("token", api.lastToken)
        assertTrue(controller.message.value!!.contains("Backup berhasil"))
    }

    @Test
    fun konsenDibatalkanTidakMengulangAksi() = runTest {
        val api = FakeDriveApi()
        api.accessTokenResult = BackupResult.ConsentRequired(Intent(Intent.ACTION_VIEW))
        val controller = newController(api, this)

        controller.startBackup()
        advanceUntilIdle()
        controller.onConsentResult(false)
        advanceUntilIdle()

        assertEquals(0, api.uploadCalls)
        assertTrue(controller.message.value!!.contains("dibatalkan"))
    }

    @Test
    fun restoreWorkspaceSamaLangsungDiterapkan() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        api.downloadResult = BackupResult.Success("""{"app":"Nyachat","format":1}""")
        var applied = false
        controller.parseRestore = { _, _ -> BackupData(emptyList(), emptyList(), familyId = "11111111") }
        controller.restoreParsedBackup = { applied = true; true }

        controller.confirmRestore(DriveBackupFile("id1", "backup.json", "2026-01-01"))
        advanceUntilIdle()

        assertTrue(applied)
        assertTrue(controller.message.value!!.contains("Backup berhasil dipulihkan"))
        assertFalse(controller.busy.value)
    }

    @Test
    fun restoreLintasWorkspaceButuhKonfirmasiEksplisit() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        api.downloadResult = BackupResult.Success("""{"app":"Nyachat","format":1}""")
        controller.parseRestore = { _, _ -> BackupData(emptyList(), emptyList(), familyId = "99999999") }

        controller.confirmRestore(DriveBackupFile("id1", "backup.json", "2026-01-01"))
        advanceUntilIdle()

        // Backup milik workspace lain → dialog konfirmasi tampil, restore belum jalan.
        assertNotNull(controller.crossFamilyRestore.value)
        assertEquals("99999999", controller.crossFamilyRestore.value!!.familyId)
        assertNull(controller.backups.value)
        assertNull(controller.restoreTarget.value)
        assertFalse(controller.busy.value)

        // Konfirmasi → restore diterapkan.
        controller.proceedCrossFamilyRestore()
        advanceUntilIdle()
        assertNull(controller.crossFamilyRestore.value)
        assertTrue(controller.message.value!!.contains("Backup berhasil dipulihkan"))
    }

    @Test
    fun batalMenghentikanOperasiDanMenutupModal() = runTest {
        val api = FakeDriveApi()
        api.hangUpload = true
        val controller = newController(api, this)

        controller.startBackup()
        advanceUntilIdle()

        assertTrue(controller.busy.value) // upload menggantung → modal tampil
        controller.cancelActiveOperation()
        advanceUntilIdle()

        assertFalse(controller.busy.value)
        assertNull(controller.message.value)
        assertNull(controller.backups.value)
    }

    @Test
    fun silentBackupMengembalikanTrueDanMenandaiSukses() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        var successStamped = false
        controller.onSuccessfulBackup = { successStamped = true }

        assertTrue(controller.silentBackup())
        assertEquals(1, api.uploadCalls)
        assertEquals(1, api.pruneCalls)
        assertTrue(successStamped)
    }

    @Test
    fun silentBackupTanpaAkunTidakMelakukanApaApa() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        controller.currentEmail = { null }

        assertFalse(controller.silentBackup())
        assertEquals(0, api.uploadCalls)
    }

    // ---- Sprint-2: backup terenkripsi ----

    @Test
    fun backupTerenkripsiMintaPassphraseDuluLaluUploadAmplop() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        controller.getEncryptionEnabled = { true }

        controller.startBackup()
        advanceUntilIdle()

        // Belum ada upload — menunggu passphrase.
        assertTrue(controller.passphrasePrompt.value is DriveBackupController.PassphrasePrompt.Backup)
        assertEquals(0, api.uploadCalls)

        controller.submitPassphrase("rahasia123")
        advanceUntilIdle()

        assertEquals(1, api.uploadCalls)
        // Yang di-upload amplop terenkripsi, BUKAN JSON plaintext.
        assertTrue(BackupCrypto.isEncryptedEnvelope(api.lastJson!!))
        assertFalse(api.lastJson!!.contains("\"app\":\"Nyachat\",\"format\""))
        // Isi amplop bisa dibuka kembali dengan passphrase yang sama.
        assertEquals(
            """{"app":"Nyachat"}""",
            BackupCrypto.decryptEnvelope(api.lastJson!!, "rahasia123")
        )
        assertTrue(controller.message.value!!.contains("Backup berhasil"))
    }

    @Test
    fun batalPassphraseMembatalkanBackup() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        controller.getEncryptionEnabled = { true }

        controller.startBackup()
        controller.cancelPassphrase()
        advanceUntilIdle()

        assertNull(controller.passphrasePrompt.value)
        assertEquals(0, api.uploadCalls)
    }

    @Test
    fun silentBackupTerenkripsiDipakaiAutoPassphrase() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        controller.getEncryptionEnabled = { true }
        // M5: auto-passphrase dari Keystore dipakai — auto-backup TETAP jalan
        // walau enkripsi aktif (sebelumnya dilewati → backup 24 jam hilang).
        controller.getAutoPassphrase = { "auto-passphrase-keystore" }
        controller.buildBackupJson = { """{"app":"Nyachat","format":1}""" }

        assertTrue(controller.silentBackup())
        assertEquals(1, api.uploadCalls)
        // Isi yang diupload harus berupa envelope terenkripsi (bukan plaintext).
        assertTrue(api.lastJson != null && BackupCrypto.isEncryptedEnvelope(api.lastJson!!))
    }

    @Test
    fun silentBackupTanpaAutoPassphraseDilewati() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        controller.getEncryptionEnabled = { true }
        // Tidak ada auto-passphrase (SecureStorage gagal) → tidak ada backup.
        controller.getAutoPassphrase = { null }

        assertFalse(controller.silentBackup())
        assertEquals(0, api.uploadCalls)
    }

    @Test
    fun restoreBackupTerenkripsiMintaPassphraseDanTolakPassphraseSalah() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        val envelope = BackupCrypto.encryptToEnvelope(
            """{"app":"Nyachat","format":1}""", "benar123", iterations = 1_000
        )
        api.downloadResult = BackupResult.Success(envelope)
        controller.parseRestore = { _, passphrase ->
            if (passphrase == "benar123") BackupData(emptyList(), emptyList(), familyId = "11111111")
            else null
        }

        controller.confirmRestore(DriveBackupFile("id1", "backup.json", "2026-01-01"))
        advanceUntilIdle()

        // Prompt passphrase muncul, restore belum jalan.
        assertTrue(controller.passphrasePrompt.value is DriveBackupController.PassphrasePrompt.Restore)

        // Passphrase salah → pesan error, tidak diterapkan.
        var applied = false
        controller.restoreParsedBackup = { applied = true; true }
        controller.submitPassphrase("salah999")
        advanceUntilIdle()
        assertFalse(applied)
        assertTrue(controller.message.value!!.contains("Passphrase salah"))
    }

    @Test
    fun restoreBackupTerenkripsiDenganPassphraseBenarDiterapkan() = runTest {
        val api = FakeDriveApi()
        val controller = newController(api, this)
        val envelope = BackupCrypto.encryptToEnvelope(
            """{"app":"Nyachat","format":1}""", "benar123", iterations = 1_000
        )
        api.downloadResult = BackupResult.Success(envelope)
        var receivedPassphrase: String? = null
        controller.parseRestore = { _, passphrase ->
            receivedPassphrase = passphrase
            BackupData(emptyList(), emptyList(), familyId = "11111111")
        }
        var applied = false
        controller.restoreParsedBackup = { applied = true; true }

        controller.confirmRestore(DriveBackupFile("id1", "backup.json", "2026-01-01"))
        advanceUntilIdle()
        controller.submitPassphrase("benar123")
        advanceUntilIdle()

        assertEquals("benar123", receivedPassphrase)
        assertTrue(applied)
        assertTrue(controller.message.value!!.contains("Backup berhasil dipulihkan"))
    }
}
