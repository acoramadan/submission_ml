package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.nfc.Tag
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.support.image.ImageProcessor
import com.dicoding.asclepius.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier


class ImageClassifierHelper(
    var threshold: Float = 0.1f,
    var maxResult: Int = 3,
    val modelName: String = "cancer_classification.tflite",
    val context: Context,
    val classifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null
    init {
        setupImageClassifier()
    }
    private fun setupImageClassifier() {
        // TODO: Menyiapkan Image Classifier untuk memproses gambar.
        val optionBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResult)
        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(4)
        optionBuilder.setBaseOptions(baseOptionsBuilder.build())

        try{
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                optionBuilder.build()
            )
        }catch (e: IllegalStateException) {
            classifierListener?.onError(
                context.getString(R.string.image_classifier_failed)
            )
            Log.e(TAG, e.message.toString())
        }
    }

    fun classifyStaticImage(imageUri: ImageProxy) {
        // TODO: mengklasifikasikan imageUri dari gambar statis.
        if(imageClassifier == null) {
            setupImageClassifier()
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224,224,ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(CastOp(DataType.UINT8))
            .build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(toBitmap(imageUri)))
        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(imageUri.imageInfo.rotationDegrees))
            .build()
        var inferenceTime = SystemClock.uptimeMillis()
        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        classifierListener?.onResults(
            results,
            inferenceTime
        )
    }

    private fun toBitmap(image: ImageProxy): Bitmap {
        val bitmapBuffer = Bitmap.createBitmap(
            image.width,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        image.close()
        return bitmapBuffer
    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when(rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }
    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}