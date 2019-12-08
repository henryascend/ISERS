package henryascend.io.isers



import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroupOverlay
import androidx.appcompat.app.AppCompatActivity
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.Facing
import henryascend.io.isers.Classifier.EmotionIdentify
import henryascend.io.isers.facedetector.FaceBoundsOverlay
import henryascend.io.isers.facedetector.FaceDetector
import henryascend.io.isers.models.Frame
import henryascend.io.isers.models.Size
import kotlinx.android.synthetic.main.activity_recognition.*


class RecognitionActivity : AppCompatActivity(){

    private lateinit var cameraView: CameraView
    private lateinit var faceBoundsOverlay: FaceBoundsOverlay
    private lateinit var emotionIdentify: EmotionIdentify

    private val faceDetector: FaceDetector by lazy{
        FaceDetector(faceBoundsOverlay)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recognition)
        cameraView = findViewById(R.id.cameraView)
       // cameraView.setLifecycleOwner(this)
        faceBoundsOverlay = findViewById(R.id.faceboundsoverlay)
        cameraView.setPreviewFrameRate(30F)
        cameraView.facing = Facing.BACK
        startProcessing()

    }

    private fun startProcessing(){

        emotionIdentify = EmotionIdentify.classifier(assets,"converted_model.tflite")

        cameraView.addFrameProcessor {
            Log.v("width & height", ""+it.size.width + " "+it.size.height)
            faceDetector.process( Frame(
                data = it.data,
                rotation = it.rotation,
                size = Size(it.size.width, it.size.height),
                format = it.format,
                isCameraFacingBack = cameraView.facing == Facing.BACK
            ), emotionIdentify
           )
        }

//        revertCameraButton.setOnClickListener {
//                cameraView.toggleFacing()
//                faceBoundsOverlay.clearFaces()
//        }
    }



    override fun onResume() {
        super.onResume()
        cameraView.open()
    }

    override fun onPause() {
        super.onPause()
        cameraView.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.destroy()
    }

}