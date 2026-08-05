package com.startupmini.nyachat.data.backup

import android.content.Context
import android.content.Intent
import com.startupmini.nyachat.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kontrol backup/restore Google Drive (P4-4) — hasil ekstraksi dari MainActivity.
 *
 * Sebelumnya ~150 baris logika (token OAuth, upload, daftar, unduh, prune, konsen,
 * restore lintas-workspace) hidup di dalam composable sehingga MainActivity sulit
 * dirawat & logikanya tidak bisa di-*unit test*. Controller ini memegang:
 * - semua state UI (busy, message, daftar backup, target restore, konsen)
 * - alur OAuth (retry otomatis setelah konsen disetujui)
 * - backup diam-diam untuk auto-backup harian ([silentBackup])
 *
 * Dependency (workspacePin, builder JSON, parser) di-assign ulang tiap komposisi
 * lewat property var — menghindari menangkap state basi di lambda remember.
 */
class DriveBackupController(
    private val scope: CoroutineScope,
    private val context: Context,
    private val driveApi: DriveBackupApi = DriveBackupManager,
    // Operasi Drive (termasuk KDF/enkripsi CPU-intensif) berjalan di IO supaya
    // tidak membekukan UI; bisa di-inject test dispatcher untuk unit test.
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // ===== Dependency yang di-wire ulang tiap komposisi =====

    /** PIN workspace aktif (untuk deteksi restore lintas-workspace). */
    var getWorkspacePin: () -> String? = { null }
    /** Email akun Google yang login (dipakai untuk token OAuth Drive). */
    var currentEmail: () -> String? =
        { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email }
    /** Membangun JSON backup lengkap (dari ViewModel). */
    var buildBackupJson: () -> String = { "" }
    /** Parse JSON backup → BackupData (null kalau rusak/format masa depan/passphrase salah). */
    var parseRestore: (String, String?) -> BackupData? = { _, _ -> null }
    /** Terapkan backup ke data lokal + cloud. */
    var restoreParsedBackup: suspend (BackupData) -> Boolean = { false }
    /** Dipanggil setiap backup berhasil (untuk menandai auto-backup terakhir). */
    var onSuccessfulBackup: () -> Unit = {}
    /** Apakah enkripsi backup aktif (dari Pengaturan). */
    var getEncryptionEnabled: () -> Boolean = { false }

    // ===== State UI =====

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _backups = MutableStateFlow<List<DriveBackupFile>?>(null)
    val backups: StateFlow<List<DriveBackupFile>?> = _backups.asStateFlow()

    private val _restoreTarget = MutableStateFlow<DriveBackupFile?>(null)
    val restoreTarget: StateFlow<DriveBackupFile?> = _restoreTarget.asStateFlow()

    private val _crossFamilyRestore = MutableStateFlow<BackupData?>(null)
    val crossFamilyRestore: StateFlow<BackupData?> = _crossFamilyRestore.asStateFlow()

    private val _consentIntent = MutableStateFlow<Intent?>(null)
    val consentIntent: StateFlow<Intent?> = _consentIntent.asStateFlow()

    /** Aksi yang sedang menunggu passphrase user (backup terenkripsi / restore
     *  backup terenkripsi). Passphrase tidak pernah disimpan — hanya hidup di
     *  memori selama alur ini. */
    sealed class PassphrasePrompt {
        data object Backup : PassphrasePrompt()
        data class Restore(val payload: String) : PassphrasePrompt()
    }

    private val _passphrasePrompt = MutableStateFlow<PassphrasePrompt?>(null)
    val passphrasePrompt: StateFlow<PassphrasePrompt?> = _passphrasePrompt.asStateFlow()

    /** Aksi yang diulang otomatis setelah user menyetujui konsen OAuth Drive. */
    private var pendingRetry: (() -> Unit)? = null

    /** Operasi Drive yang sedang berjalan — dibatalkan lewat tombol Batal (B3). */
    private var activeJob: Job? = null

    /**
     * Luncurkan operasi Drive. Operasi sebelumnya dibatalkan (tidak mungkin ada
     * dua modal backup sekaligus). Job disimpan supaya [cancelActiveOperation]
     * bisa menghentikannya.
     *
     * Busy di-reset di sini (bukan di tiap operasi) dengan guard identitas job:
     * `finally` dari operasi yang sudah dibatalkan TIDAK boleh menutup modal
     * operasi baru yang dimulai user setelah Batal (race B3).
     */
    private fun launchOperation(block: suspend CoroutineScope.() -> Unit) {
        activeJob?.cancel()
        val job = scope.launch(workDispatcher) {
            // Daftar dulu dari dalam: pada dispatcher unconfined (unit test)
            // blok bisa selesai SEBELUM `activeJob = job` di luar terlaksana —
            // tanpa ini guard `finally` gagal me-reset busy.
            activeJob = coroutineContext[Job]
            try {
                block()
            } finally {
                if (activeJob == coroutineContext[Job]) {
                    _busy.value = false
                }
            }
        }
        activeJob = job
    }

    /**
     * Batalkan operasi Drive yang sedang berjalan (tombol Batal di modal).
     * Modal langsung ditutup; panggilan jaringan yang tersisa berakhir sendiri
     * di background sampai timeout OkHttp.
     */
    fun cancelActiveOperation() {
        activeJob?.cancel()
        activeJob = null
        pendingRetry = null
        _busy.value = false
        _backups.value = null
        _restoreTarget.value = null
    }

    // ===== Backup =====

    /**
     * Backup penuh dengan feedback UI (dipicu dari menu Pengaturan).
     * Enkripsi aktif → minta passphrase dulu; passphrase dipakai mengenkripsi
     * isi backup sebelum upload dan TIDAK pernah disimpan.
     */
    fun startBackup() {
        if (getEncryptionEnabled()) {
            _passphrasePrompt.value = PassphrasePrompt.Backup
            return
        }
        runBackup(encryptedPassphrase = null)
    }

    private fun runBackup(encryptedPassphrase: String?) {
        launchOperation {
            _busy.value = true
            val token = driveToken { runBackup(encryptedPassphrase) } ?: return@launchOperation
            val plain = buildBackupJson()
            val json = encryptedPassphrase?.let { BackupCrypto.encryptToEnvelope(plain, it) } ?: plain
            val fileName = "Nyachat-backup-${timestampForFile()}.json"
            val uploadResult = driveApi.uploadBackup(context, token, fileName, json)
            when (uploadResult) {
                is BackupResult.Success -> {
                    driveApi.pruneOldBackups(context, token, 5)
                    onSuccessfulBackup()
                    _message.value = context.getString(R.string.backup_success, fileName)
                }
                else -> _message.value = context.getString(R.string.backup_failed)
            }
        }
    }

    /**
     * Backup diam-diam untuk auto-backup harian (24 jam) saat app dibuka.
     * Tanpa feedback UI & tanpa memunculkan dialog konsen — kalau user belum
     * pernah menyetujui akses Drive, operasi dilewati. Return true kalau berhasil.
     */
    suspend fun silentBackup(): Boolean {
        // Backup terenkripsi butuh passphrase interaktif — auto-backup tidak
        // pernah menyimpan passphrase, jadi dilewati saat enkripsi aktif.
        if (getEncryptionEnabled()) return false
        val email = currentEmail() ?: return false
        return when (val tokenResult = driveApi.getAccessToken(context, email)) {
            is BackupResult.Success -> {
                val token = tokenResult.value
                val json = buildBackupJson()
                val uploadResult = driveApi.uploadBackup(
                    context, token, "Nyachat-backup-${timestampForFile()}.json", json
                )
                if (uploadResult is BackupResult.Success) {
                    driveApi.pruneOldBackups(context, token, 5)
                    onSuccessfulBackup()
                    true
                } else {
                    false
                }
            }
            // ConsentRequired/Failure/NotFound/QuotaExceeded — dilewati diam-diam.
            else -> false
        }
    }

    // ===== Restore =====

    /** Ambil daftar backup (5 terbaru) untuk ditampilkan di dialog pemilih. */
    fun startRestore() {
        launchOperation {
            _busy.value = true
            val token = driveToken { startRestore() } ?: return@launchOperation
            val filesResult = driveApi.listBackups(context, token)
            when (filesResult) {
                is BackupResult.Success -> {
                    val files = filesResult.value
                    if (files.isEmpty()) {
                        _message.value = context.getString(R.string.restore_no_backup)
                    } else {
                        _backups.value = files.take(5)
                    }
                }
                else -> _message.value = context.getString(R.string.restore_failed)
            }
        }
    }

    /** Unduh & siapkan restore file [file] (konfirmasi lintas-workspace jika perlu). */
    fun confirmRestore(file: DriveBackupFile) {
        launchOperation {
            _busy.value = true
            val token = driveToken { confirmRestore(file) } ?: return@launchOperation
            val downloadResult = driveApi.downloadBackup(context, token, file.fileId)
            // Tutup dialog pemilih & konfirmasi file — file sudah terunduh;
            // alur lanjut di handleDownloadedBackup (termasuk prompt passphrase
            // bila backup terenkripsi, atau konfirmasi lintas-workspace).
            _backups.value = null
            _restoreTarget.value = null
            when (downloadResult) {
                is BackupResult.Success -> handleDownloadedBackup(downloadResult.value, null)
                else -> _message.value = context.getString(R.string.restore_failed)
            }
        }
    }

    /**
     * Proses isi backup yang sudah terunduh. Backup terenkripsi tanpa
     * [passphrase] → munculkan prompt passphrase; passphrase salah → pesan error.
     */
    private suspend fun handleDownloadedBackup(payload: String, passphrase: String?) {
        if (passphrase == null && BackupCrypto.isEncryptedEnvelope(payload)) {
            _passphrasePrompt.value = PassphrasePrompt.Restore(payload)
            return
        }
        val data = parseRestore(payload, passphrase)
        when {
            data == null -> {
                _message.value = context.getString(
                    if (passphrase != null) R.string.restore_wrong_passphrase
                    else R.string.restore_failed_parse
                )
            }
            data.familyId != null && data.familyId != getWorkspacePin() -> {
                // Backup milik workspace lain → konfirmasi eksplisit dulu
                // (restore akan menimpa lokal + push ke workspace ini).
                _crossFamilyRestore.value = data
            }
            else -> doRestore(data)
        }
    }

    /**
     * Passphrase dari dialog: lanjutkan backup terenkripsi atau buka backup
     * terenkripsi yang sedang di-restore.
     */
    fun submitPassphrase(value: String) {
        val prompt = _passphrasePrompt.value ?: return
        _passphrasePrompt.value = null
        when (prompt) {
            is PassphrasePrompt.Backup -> runBackup(value)
            // KDF PBKDF2 + dekripsi CPU-intensif — wajib lewat launchOperation
            // (Dispatchers.IO) supaya tidak membekukan UI / ANR.
            is PassphrasePrompt.Restore -> launchOperation {
                _busy.value = true
                handleDownloadedBackup(prompt.payload, value)
            }
        }
    }

    fun cancelPassphrase() {
        _passphrasePrompt.value = null
    }

    /** Lanjutkan restore backup lintas-workspace setelah konfirmasi user. */
    fun proceedCrossFamilyRestore() {
        val data = _crossFamilyRestore.value ?: return
        doRestore(data)
    }

    fun cancelCrossFamilyRestore() {
        _crossFamilyRestore.value = null
    }

    /** Terapkan hasil parse backup ke data lokal + cloud. */
    private fun doRestore(data: BackupData) {
        launchOperation {
            _busy.value = true
            val ok = restoreParsedBackup(data)
            _message.value = context.getString(
                if (ok) R.string.restore_success else R.string.restore_failed_parse
            )
            _backups.value = null
            _restoreTarget.value = null
            _crossFamilyRestore.value = null
        }
    }

    // ===== Token & konsen OAuth Drive =====

    /**
     * Ambil token Drive; kalau butuh persetujuan user, simpan intent konsen +
     * aksi retry supaya aksi diulang otomatis setelah dialog konsen disetujui.
     */
    private suspend fun driveToken(retry: () -> Unit): String? {
        val email = currentEmail()
        if (email == null) {
            _message.value = context.getString(R.string.drive_err_not_signed_in)
            return null
        }
        val tokenResult = driveApi.getAccessToken(context, email)
        return when (tokenResult) {
            is BackupResult.Success -> tokenResult.value
            is BackupResult.ConsentRequired -> {
                _consentIntent.value = tokenResult.intent
                pendingRetry = retry
                null
            }
            else -> {
                _message.value = context.getString(
                    R.string.drive_err_token, tokenResult.errorMessage ?: "Unknown error"
                )
                null
            }
        }
    }

    /** Dipanggil UI tepat sebelum intent konsen dibuka — cegah re-launch ganda. */
    fun consumeConsentIntent() {
        _consentIntent.value = null
    }

    /**
     * Hasil dialog konsen OAuth: OK → ulangi aksi yang tertunda; batal → tampilkan
     * informasi (tanpa mengulang supaya tidak muncul dialog berulang-ulang).
     */
    fun onConsentResult(ok: Boolean) {
        if (ok) {
            pendingRetry?.invoke()
        } else {
            _message.value = context.getString(R.string.drive_consent_cancelled)
        }
        pendingRetry = null
    }

    // ===== Pesan info transien =====

    /** Tampilkan pesan info transien (dialog "Informasi"). */
    fun showMessage(text: String?) {
        _message.value = text
    }

    // ===== Dismiss dari dialog =====

    fun dismissMessage() { _message.value = null }
    fun dismissBackups() { _backups.value = null }
    fun dismissRestoreTarget() { _restoreTarget.value = null }
}

/** Nama file dengan timestamp: 20260803-143000 */
private fun timestampForFile(): String =
    SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
