package henryascend.io.isers.facedetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import henryascend.io.isers.models.FaceBounds
import henryascend.io.isers.models.Facing
import henryascend.io.isers.models.Orientation


/**
 * A view that renders the results of a face detection process. It contains a list of faces
 * bounds which it draws using a set of attributes provided by the camera: Its width, height,
 * orientation and facing. These attributes impact how the face bounds are drawn, especially
 * the scaling factor between this view and the camera view, and the mirroring of coordinates.
 * It was created by zaid with the help of the Google's GraphicOverlay sample here:
 *
 * https://github.com/googlesamples/android-vision/blob/master/visionSamples/FaceTracker/app/src/main/java/com/google/android/gms/samples/vision/face/facetracker/ui/camera/GraphicOverlay.java
 *
 */

class FaceBoundsOverlay @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet ?= null,
    defStyleAttr: Int = 0) : View(ctx, attrs, defStyleAttr){

    private val faceBounds : MutableList<FaceBounds> = mutableListOf()

    private val anchorPaint = Paint()
    private val idPaint = Paint()
    private val boundsPaint0 = Paint()
    private val boundsPaint1 = Paint()

    var cameraPreviewWidth : Float = 0f
    var cameraPreviewHeight : Float = 0f
    var cameraOrientation: Orientation = Orientation.ANGLE_270
    var cameraFacing: Facing = Facing.BACK

    val emotionlabel = arrayOf("Angry", "Disgust","Fear", "Happy", "Sad", "Surprise", "Neutral")



    init{
        anchorPaint.color = ContextCompat.getColor(context,android.R.color.white)

        idPaint.color = ContextCompat.getColor(context, android.R.color.white)
        idPaint.textSize = 40f


        boundsPaint1.style = Paint.Style.STROKE
        boundsPaint1.color = ContextCompat.getColor(context, android.R.color.holo_red_light)
        boundsPaint1.strokeWidth = 4f

        boundsPaint0.style = Paint.Style.STROKE
        boundsPaint0.color = ContextCompat.getColor(context, android.R.color.white)
        boundsPaint0.strokeWidth = 4f
    }

    /**
     * Repopulates the face bounds list, and calls for a redraw of the view.
     */

    fun updateFaces(bounds: List <FaceBounds>){
        faceBounds.clear()
        faceBounds.addAll(bounds)
        invalidate()
    }

    fun clearFaces(){
        faceBounds.clear()
        idPaint.clearShadowLayer()
        boundsPaint0.clearShadowLayer()
        boundsPaint1.clearShadowLayer()
    }

    override fun onDraw(canvas: Canvas){
        super.onDraw(canvas)
        Log.v("facing",""+cameraFacing)
        Log.v("orientation",""+cameraOrientation)
        faceBounds.forEach {
            val centerX = computeFaceBoundsCenterX(width.toFloat() ,  scaleX(it.box.exactCenterX(), canvas) )
            val centerY = computeFaceBoundsCenterY(height.toFloat(), scaleY(it.box.exactCenterY(), canvas) )
            drawAnchor(canvas,centerX,centerY)
            if (it.sleepy > 0.10)
                 drawId(canvas,it.id.toString(),centerX,centerY,emotionlabel[it.status] )
            drawBounds(it.box,canvas,centerX, centerY, it.sleepy)
        }


    }

    /**
     * Calculates the center of the face bounds's X coordinate depending on the camera's facing
     * and orientation. A change in the facing results in mirroring the coordinate.
     */

    private fun computeFaceBoundsCenterX(viewWidth: Float, scaledCenterX: Float) = when {
        cameraFacing == Facing.FRONT && cameraOrientation == Orientation.ANGLE_270 -> viewWidth - scaledCenterX
        cameraFacing == Facing.FRONT && cameraOrientation == Orientation.ANGLE_180 ->viewWidth - scaledCenterX
        cameraFacing == Facing.FRONT && cameraOrientation == Orientation.ANGLE_90 ->  scaledCenterX
        cameraFacing == Facing.FRONT && cameraOrientation == Orientation.ANGLE_0 ->  viewWidth - scaledCenterX


        cameraFacing == Facing.BACK && cameraOrientation == Orientation.ANGLE_90 ->  scaledCenterX
        cameraFacing == Facing.BACK && cameraOrientation == Orientation.ANGLE_180 ->  scaledCenterX
        cameraFacing == Facing.BACK && cameraOrientation == Orientation.ANGLE_0 ->  scaledCenterX
        cameraFacing == Facing.BACK && cameraOrientation == Orientation.ANGLE_270 -> viewWidth -  scaledCenterX

        else -> scaledCenterX
    }

    /**
     * Calculates the center of the face bounds's Y coordinate depending on the camera's facing
     * and orientation. A change in the facing results in mirroring the coordinate.
     */
    private fun computeFaceBoundsCenterY(viewHeight: Float, scaledCenterY: Float) = when {
        cameraFacing == Facing.FRONT && cameraOrientation == Orientation.ANGLE_270 -> scaledCenterY
        cameraFacing == Facing.FRONT && cameraOrientation == Orientation.ANGLE_180 -> scaledCenterY
        cameraFacing == Facing.FRONT && cameraOrientation == Orientation.ANGLE_90 -> viewHeight - scaledCenterY
        cameraFacing == Facing.FRONT && cameraOrientation == Orientation.ANGLE_0 ->  scaledCenterY

        cameraFacing == Facing.BACK && cameraOrientation == Orientation.ANGLE_90 -> scaledCenterY
        cameraFacing == Facing.BACK && cameraOrientation == Orientation.ANGLE_180 -> scaledCenterY
        cameraFacing == Facing.BACK && cameraOrientation == Orientation.ANGLE_0 -> scaledCenterY
        cameraFacing == Facing.BACK && cameraOrientation == Orientation.ANGLE_270 -> viewHeight - scaledCenterY
        else -> scaledCenterY
    }

    /**
     * Draws an anchor/dot at the center of a face
     */
    private fun drawAnchor(canvas: Canvas, centerX: Float, centerY: Float) {
        canvas.drawCircle(
            centerX,
            centerY,
            ANCHOR_RADIUS,
            anchorPaint)
    }

    private fun drawId(canvas: Canvas, id: String, centerX: Float, centerY: Float, emotion: String) {
            canvas.drawText(
                "id $id : $emotion",
                centerX - ID_OFFSET* 2.1f,
                centerY + ID_OFFSET,
                idPaint)
    }

    private fun drawBounds(box: Rect, canvas: Canvas, centerX: Float, centerY: Float, sleepy :Float) {
        val xOffset = scaleX(box.width() / 2.0f, canvas)
        val yOffset = scaleY(box.height() / 2.0f, canvas)
        val left = centerX - xOffset
        val right = centerX + xOffset
        val top = centerY - yOffset
        val bottom = centerY + yOffset
        if (sleepy > 0.1)
            canvas.drawRect(
                left,
                top,
                right,
                bottom,
                boundsPaint0)
        else
            canvas.drawRect(
                left,
                top,
                right,
                bottom,
                boundsPaint1)
    }

    /**
     * Adjusts a horizontal value x from the camera preview scale to the view scale
     */
    private fun scaleX(x: Float, canvas: Canvas) =
        x * (canvas.width.toFloat() / cameraPreviewWidth)

    /**
     * Adjusts a vertical value y from the camera preview scale to the view scale
     */
    private fun scaleY(y: Float, canvas: Canvas) =
        y * (canvas.height.toFloat() / cameraPreviewHeight)

    companion object {
        private const val ANCHOR_RADIUS = 5f
        private const val ID_OFFSET = 50f
    }
}
