package henryascend.io.isers.facedetector

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import henryascend.io.isers.Classifier.EmotionIdentify
import henryascend.io.isers.models.FaceBounds
import henryascend.io.isers.models.Frame
import kotlin.math.max
import kotlin.math.min


class FaceDetector(private val faceBoundsOverlay: FaceBoundsOverlay){

    private val faceBoundsOverlayHandle = FaceBoundsOverlayHandler()
    private val firebaseFaceDetectorWrapper = FirebaseFaceDetectorWrapper()
    private lateinit var classifier_model: EmotionIdentify



    fun process(frame: Frame, model : EmotionIdentify){
        updateOverlayAttributes(frame)
        detectFacesIn(frame)
        classifier_model = model
    }

    private fun updateOverlayAttributes(frame: Frame){
        faceBoundsOverlayHandle.updateOverlayAttributes(
            overlayWidth = frame.size.width,
            overlayHeight = frame.size.height,
            rotation = frame.rotation,
            isCameraFacingBack = frame.isCameraFacingBack,
            callback = { newWidth, newHeight, newOrientation, newFacing ->
                faceBoundsOverlay.cameraPreviewWidth = newWidth
                faceBoundsOverlay.cameraPreviewHeight = newHeight
                faceBoundsOverlay.cameraOrientation = newOrientation
                faceBoundsOverlay.cameraFacing = newFacing
            }
        )
    }

    private fun detectFacesIn(frame: Frame){
        frame.data?.let {
            var full_image = convertFrameToImage(frame)
            firebaseFaceDetectorWrapper.process(
                image = convertFrameToImage(frame),
                onSuccess = {
                    faceBoundsOverlay.updateFaces(convertToListOfFaceBounds(it, full_Image = full_image))
                },
                onError = {
                    Toast.makeText(faceBoundsOverlay.context, "Error processing images: $it", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun convertFrameToImage(frame: Frame) =
        FirebaseVisionImage.fromByteArray(frame.data!!, extractFrameMetadata(frame))

    private fun extractFrameMetadata(frame: Frame): FirebaseVisionImageMetadata =
        FirebaseVisionImageMetadata.Builder()
            .setWidth(frame.size.width)
            .setHeight(frame.size.height)
            .setFormat(frame.format)
            .setRotation(frame.rotation / RIGHT_ANGLE)
            .build()

    private fun convertToListOfFaceBounds(faces: MutableList<FirebaseVisionFace>, full_Image: FirebaseVisionImage): List<FaceBounds> =
        faces.map { FaceBounds(it.trackingId, it.boundingBox, getEmotionStatus(it.boundingBox, full_Image)  ) }


    private fun getEmotionStatus( box: Rect, image : FirebaseVisionImage ) : Int{
        val x =  max(box.centerX()-box.width()/2,0)
        val y = max(box.centerY()-box.height()/2, 0)
        var width : Int
        var height : Int
        if (image.bitmap.width - x >= box.width())
           width = box.width()
        else
            width = image.bitmap.width - x

        if (image.bitmap.height - y >= box.height())
            height = box.height()
        else
            height= image.bitmap.height - y
        var bitmap = Bitmap.createBitmap(image.bitmap, x, y, width,height)
        var reshape =  Bitmap.createScaledBitmap(bitmap,48,48, true)
        var recognition = classifier_model.recognizeImage(reshape)
        Log.v("recognition", ""+recognition)
        return recognition
    }

    companion object {
        private const val RIGHT_ANGLE = 90
    }
}