package com.bambiproj.docscanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Result of an auto-crop attempt: the (possibly cropped) bitmap and whether a page was found. */
data class CropResult(val bitmap: Bitmap, val cropped: Boolean)

/**
 * Finds the largest document-like quadrilateral in a photo and warps it to a
 * flat, rectangular page — the "automatic cropping" a scanner app does. Runs
 * entirely on-device with OpenCV. If no confident page outline is found, the
 * original bitmap is returned untouched.
 */
object DocumentCropper {

    // Detection runs on a downscaled copy for speed; the final warp uses full res.
    private const val PROCESS_MAX_EDGE = 1000.0

    fun cropToDocument(source: Bitmap): CropResult {
        val full = Mat()
        val gray = Mat()
        val edges = Mat()
        val hierarchy = Mat()
        try {
            Utils.bitmapToMat(source, full) // CV_8UC4 (RGBA)

            val longest = max(source.width, source.height).toDouble()
            val procScale = if (longest > PROCESS_MAX_EDGE) PROCESS_MAX_EDGE / longest else 1.0

            Imgproc.cvtColor(full, gray, Imgproc.COLOR_RGBA2GRAY)
            if (procScale < 1.0) {
                Imgproc.resize(
                    gray, gray,
                    Size(source.width * procScale, source.height * procScale)
                )
            }
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
            Imgproc.Canny(gray, edges, 60.0, 180.0)
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(edges, edges, kernel)

            val contours = ArrayList<MatOfPoint>()
            Imgproc.findContours(
                edges, contours, hierarchy,
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE
            )

            val procArea = edges.width().toDouble() * edges.height().toDouble()
            var bestQuad: Array<Point>? = null
            var bestArea = 0.0

            for (contour in contours) {
                val c2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(c2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
                if (approx.total() == 4L) {
                    val pts = approx.toArray()
                    val quad = MatOfPoint(*pts)
                    val area = Imgproc.contourArea(approx)
                    // Keep the biggest convex 4-gon covering a fair chunk of the frame.
                    if (Imgproc.isContourConvex(quad) && area > bestArea && area > 0.18 * procArea) {
                        bestArea = area
                        bestQuad = pts
                    }
                    quad.release()
                }
                c2f.release()
                approx.release()
                contour.release()
            }

            val quad = bestQuad ?: return CropResult(source, false)

            // Map detection-space points back to full-resolution coordinates.
            val invScale = 1.0 / procScale
            val ordered = orderCorners(quad.map { Point(it.x * invScale, it.y * invScale) })

            val widthTop = distance(ordered[0], ordered[1])
            val widthBottom = distance(ordered[3], ordered[2])
            val heightLeft = distance(ordered[0], ordered[3])
            val heightRight = distance(ordered[1], ordered[2])
            val outW = max(widthTop, widthBottom)
            val outH = max(heightLeft, heightRight)
            if (outW < 32 || outH < 32) return CropResult(source, false)

            val srcQuad = MatOfPoint2f(ordered[0], ordered[1], ordered[2], ordered[3])
            val dstQuad = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(outW - 1, 0.0),
                Point(outW - 1, outH - 1),
                Point(0.0, outH - 1)
            )
            val transform = Imgproc.getPerspectiveTransform(srcQuad, dstQuad)
            val output = Mat()
            Imgproc.warpPerspective(full, output, transform, Size(outW, outH))

            val result = Bitmap.createBitmap(output.width(), output.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(output, result)

            srcQuad.release()
            dstQuad.release()
            transform.release()
            output.release()
            return CropResult(result, true)
        } catch (e: Throwable) {
            return CropResult(source, false)
        } finally {
            full.release()
            gray.release()
            edges.release()
            hierarchy.release()
        }
    }

    /** Orders four corners as [top-left, top-right, bottom-right, bottom-left]. */
    private fun orderCorners(points: List<Point>): Array<Point> {
        val tl = points.minByOrNull { it.x + it.y }!!
        val br = points.maxByOrNull { it.x + it.y }!!
        val tr = points.minByOrNull { it.y - it.x }!!
        val bl = points.maxByOrNull { it.y - it.x }!!
        return arrayOf(tl, tr, br, bl)
    }

    private fun distance(a: Point, b: Point): Double = hypot(a.x - b.x, a.y - b.y)
}
