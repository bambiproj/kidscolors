package com.bambiproj.docscanner

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.bambiproj.docscanner.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Captured pages, in order. Kept in memory until saved or cleared.
    private val pages = mutableListOf<Bitmap>()

    // The temp file the camera app is currently writing into.
    private var pendingCaptureFile: File? = null

    // The last PDF we wrote to Downloads, for the Open/Share buttons.
    private var lastSavedUri: Uri? = null

    // Image processing (crop/enhance) runs off the UI thread.
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private var openCvReady = false

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            onPhotoCaptured(success)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addPageButton.setOnClickListener { addPage() }
        binding.saveButton.setOnClickListener { savePdf() }
        binding.clearButton.setOnClickListener { clearPages() }
        binding.openButton.setOnClickListener { openLastPdf() }
        binding.shareButton.setOnClickListener { shareLastPdf() }

        openCvReady = try {
            OpenCVLoader.initLocal()
        } catch (e: Throwable) {
            false
        }

        refreshUi()
    }

    override fun onDestroy() {
        ioExecutor.shutdown()
        super.onDestroy()
    }

    // ---- Capture --------------------------------------------------------

    private fun addPage() {
        val dir = File(cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "cap_${System.currentTimeMillis()}.jpg")
        pendingCaptureFile = file
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        try {
            takePicture.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.no_camera, Toast.LENGTH_LONG).show()
        }
    }

    private fun onPhotoCaptured(success: Boolean) {
        val file = pendingCaptureFile
        pendingCaptureFile = null
        if (!success || file == null || !file.exists()) {
            setStatus(getString(R.string.status_capture_cancelled))
            return
        }
        val doEnhance = binding.enhanceSwitch.isChecked
        binding.addPageButton.isEnabled = false
        setStatus(getString(R.string.status_processing))

        ioExecutor.execute {
            var finalBmp: Bitmap? = null
            var didCrop = false
            try {
                var bmp = decodeDownsampled(file, MAX_EDGE)
                bmp = applyExifRotation(file, bmp)
                if (openCvReady) {
                    val cr = DocumentCropper.cropToDocument(bmp)
                    if (cr.cropped && cr.bitmap !== bmp) bmp.recycle()
                    bmp = cr.bitmap
                    didCrop = cr.cropped
                }
                finalBmp = if (doEnhance) enhance(bmp) else bmp
            } catch (e: Throwable) {
                finalBmp = null
            } finally {
                file.delete()
            }

            val produced = finalBmp
            val cropped = didCrop
            runOnUiThread {
                binding.addPageButton.isEnabled = true
                if (produced == null) {
                    setStatus(getString(R.string.status_capture_failed))
                } else {
                    pages.add(produced)
                    setStatus(
                        getString(
                            if (cropped) R.string.status_page_cropped
                            else R.string.status_page_full
                        )
                    )
                    refreshUi()
                }
            }
        }
    }

    /** Decodes a JPEG capped to [maxEdge] px on its longest side to avoid OOM. */
    private fun decodeDownsampled(file: File, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > maxEdge) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: throw IllegalStateException("decode failed")
    }

    /** Rotates the bitmap to match the photo's EXIF orientation. */
    private fun applyExifRotation(file: File, bmp: Bitmap): Bitmap {
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bmp
        }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        if (rotated != bmp) bmp.recycle()
        return rotated
    }

    /** Grayscale + contrast bump so pages look "scanned". */
    private fun enhance(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val contrast = 1.35f
        val translate = (-.5f * contrast + .5f) * 255f
        val gray = ColorMatrix().apply { setSaturation(0f) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        gray.postConcat(contrastMatrix)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(gray)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        if (src != out) src.recycle()
        return out
    }

    // ---- Save to PDF ----------------------------------------------------

    private fun savePdf() {
        if (pages.isEmpty()) return
        val fileName = "Scan_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".pdf"
        val uri = writePdfToDownloads(fileName)
        if (uri != null) {
            lastSavedUri = uri
            binding.openButton.isEnabled = true
            binding.shareButton.isEnabled = true
            setStatus(getString(R.string.status_saved, fileName, pages.size))
        } else {
            setStatus(getString(R.string.status_save_failed))
        }
    }

    private fun writePdfToDownloads(fileName: String): Uri? {
        val document = PdfDocument()
        try {
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            pages.forEachIndexed { index, bmp ->
                val info = PdfDocument.PageInfo
                    .Builder(PAGE_W, PAGE_H, index + 1)
                    .create()
                val page = document.startPage(info)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                val contentW = PAGE_W - 2 * MARGIN
                val contentH = PAGE_H - 2 * MARGIN
                val scale = min(
                    contentW.toFloat() / bmp.width,
                    contentH.toFloat() / bmp.height
                )
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                val left = (PAGE_W - drawW) / 2f
                val top = (PAGE_H - drawH) / 2f
                canvas.drawBitmap(bmp, null, RectF(left, top, left + drawW, top + drawH), paint)
                document.finishPage(page)
            }

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val collection =
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val target = contentResolver.insert(collection, values) ?: return null
            contentResolver.openOutputStream(target)?.use { out ->
                document.writeTo(out)
            } ?: return null
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(target, values, null, null)
            return target
        } catch (e: Exception) {
            return null
        } finally {
            document.close()
        }
    }

    // ---- Open / Share / Clear ------------------------------------------

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

    private fun clearPages() {
        pages.forEach { it.recycle() }
        pages.clear()
        lastSavedUri = null
        binding.openButton.isEnabled = false
        binding.shareButton.isEnabled = false
        setStatus(getString(R.string.status_cleared))
        refreshUi()
    }

    // ---- UI -------------------------------------------------------------

    private fun refreshUi() {
        val count = pages.size
        binding.saveButton.isEnabled = count > 0
        binding.clearButton.isEnabled = count > 0
        binding.emptyText.visibility = if (count == 0) View.VISIBLE else View.GONE
        binding.pageCount.text = resources.getQuantityString(R.plurals.page_count, count, count)

        binding.thumbsContainer.removeAllViews()
        val sizePx = (96 * resources.displayMetrics.density).toInt()
        val marginPx = (8 * resources.displayMetrics.density).toInt()
        pages.forEach { bmp ->
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).also {
                    it.marginEnd = marginPx
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(bmp)
                setBackgroundResource(R.drawable.thumb_frame)
            }
            binding.thumbsContainer.addView(iv)
        }
    }

    private fun setStatus(text: String) {
        binding.statusText.text = text
    }

    companion object {
        // Longest edge kept for each captured page (px).
        private const val MAX_EDGE = 2200
        // A4 at ~150 dpi, in points used by PdfDocument.
        private const val PAGE_W = 1240
        private const val PAGE_H = 1754
        private const val MARGIN = 48
    }
}
