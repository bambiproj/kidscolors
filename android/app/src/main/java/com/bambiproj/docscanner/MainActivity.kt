package com.bambiproj.docscanner

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bambiproj.docscanner.databinding.ActivityMainBinding
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // The last PDF we saved into the public Downloads folder, kept so the
    // user can immediately open or share it after a scan.
    private var lastSavedUri: Uri? = null

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            handleScanResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scanButton.setOnClickListener { startScan() }
        binding.openButton.setOnClickListener { openLastPdf() }
        binding.shareButton.setOnClickListener { shareLastPdf() }

        setStatus(getString(R.string.status_ready))
    }

    private fun startScan() {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(30)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()

        setStatus(getString(R.string.status_starting))
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                setStatus(getString(R.string.status_error, e.localizedMessage ?: "unknown"))
                Toast.makeText(this, R.string.scanner_unavailable, Toast.LENGTH_LONG).show()
            }
    }

    private fun handleScanResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) {
            setStatus(getString(R.string.status_cancelled))
            return
        }

        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
        val pdf = scanResult?.pdf
        if (pdf == null) {
            setStatus(getString(R.string.status_no_pdf))
            return
        }

        val fileName = "Scan_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".pdf"

        val saved = savePdfToDownloads(pdf.uri, fileName)
        if (saved != null) {
            lastSavedUri = saved
            binding.openButton.isEnabled = true
            binding.shareButton.isEnabled = true
            setStatus(getString(R.string.status_saved, fileName, pdf.pageCount))
        } else {
            setStatus(getString(R.string.status_save_failed))
        }
    }

    /**
     * Copies the temporary PDF produced by the scanner into the public
     * Downloads folder (via MediaStore) so it survives after the app closes.
     * Requires no runtime permission on Android 10+ (our minimum).
     */
    private fun savePdfToDownloads(sourceUri: Uri, fileName: String): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val collection =
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val target = contentResolver.insert(collection, values) ?: return null
            contentResolver.openInputStream(sourceUri)?.use { input ->
                contentResolver.openOutputStream(target)?.use { output ->
                    input.copyTo(output)
                }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(target, values, null, null)
            target
        } catch (e: Exception) {
            null
        }
    }

    private fun openLastPdf() {
        val uri = lastSavedUri ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.no_pdf_viewer, Toast.LENGTH_LONG).show()
        }
    }

    private fun shareLastPdf() {
        val uri = lastSavedUri ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_pdf)))
    }

    private fun setStatus(text: String) {
        binding.statusText.text = text
    }
}
