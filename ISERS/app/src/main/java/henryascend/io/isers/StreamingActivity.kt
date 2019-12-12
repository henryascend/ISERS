package henryascend.io.isers

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.controls.Facing
import henryascend.io.isers.Classifier.EmotionIdentify
import henryascend.io.isers.facedetector.FaceBoundsOverlay
import henryascend.io.isers.facedetector.FaceDetector
import henryascend.io.isers.facedetector.FaceDetector1
import henryascend.io.isers.models.Frame
import henryascend.io.isers.models.Size
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.net.Socket

class StreamingActivity : AppCompatActivity() {
    private var recording = false
    private lateinit var socket: Socket

    private lateinit var hostField: EditText
    private lateinit var portField: EditText
    private lateinit var startButton: Button
    private lateinit var resultsLabel: TextView


    private lateinit var cameraView: CameraView
    private lateinit var faceBoundsOverlay: FaceBoundsOverlay
    private lateinit var emotionIdentify: EmotionIdentify

    private val faceDetector: FaceDetector1 by lazy{
        FaceDetector1(faceBoundsOverlay)
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming)

        cameraView = findViewById(R.id.cameraView)
        cameraView.setLifecycleOwner(this)
        faceBoundsOverlay = findViewById(R.id.faceboundsoverlay)
        cameraView.setPreviewFrameRate(30F)
        cameraView.facing = Facing.BACK


        this.checkPermissions()

        hostField = findViewById(R.id.host)
        portField = findViewById(R.id.port)
        startButton = findViewById(R.id.start)
        resultsLabel = findViewById(R.id.results)

        emotionIdentify = EmotionIdentify.classifier(assets,"converted_model.tflite")



    }


    fun startClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (this.recording) {
            this.stop()
        } else {
            this.start()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),0)
        }
    }

    private fun connect() {
        this.socket = Socket(hostField.text.toString(), portField.text.toString().toInt())
    }

    private fun disconnect() {
        if (!this.socket.isOutputShutdown) {
            this.socket.shutdownOutput()
        }
    }

    private fun start() {
        this.startButton.text = "Stop"
        this.hostField.isEnabled = false
        this.portField.isEnabled = false
        this.recording = true

        AsyncTask.execute {
            try {

//
//                val output = socket.getOutputStream()


                var action = System.currentTimeMillis()

                //output.write("${action}\u0004".toByteArray(Charsets.UTF_8))
                //output.flush()

                try {
                    this.cameraView.addFrameProcessor {
                        Log.v("width & height", ""+it.size.width + " "+it.size.height)
                        faceDetector.process( Frame(
                            data = it.data,
                            rotation = it.rotation,
                            size = Size(it.size.width, it.size.height),
                            format = it.format,
                            isCameraFacingBack = cameraView.facing == Facing.BACK
                        ), emotionIdentify)
                        faceDetector.getFaceImages().forEach {
                            this.connect()
                            val output = socket.getOutputStream()
                            Log.v("bytearray",""+it)
                            output.write(it)
                            output.flush()
                            output.close()
                            this.disconnect()
                        }
                    }
                } catch (err: Exception) {
                    runOnUiThread {
                        this.stop()
                        resultsLabel.text = "Failed to record:\n" +
                                "${getStack(err)}\n\n" +
                                "${resultsLabel.text}"
                    }
                }

//                try {
//                    this.disconnect()
//                    //output.close()
//                    //faceBoundsOverlay.clearFaces()
//                    //cameraView.clearFrameProcessors()
//                } catch (err: Exception) {
//                    runOnUiThread {
//                        this.stop()
//                        resultsLabel.text = "Failed to disconnect socket:\n" +
//                                "${getStack(err)}\n\n" +
//                                "${resultsLabel.text}"
//                    }
//                }

            } catch (err: Exception) {
                runOnUiThread {
                    this.stop()
                    resultsLabel.text = "Failed to connect socket:\n" +
                            "${getStack(err)}\n\n" +
                            "${resultsLabel.text}"
                }
            }
        }
    }


    private fun stop() {
        this.recording = false
        this.startButton.text = "Start"
        this.hostField.isEnabled = true
        this.portField.isEnabled = true
        faceBoundsOverlay.clearFaces()
        cameraView.clearFrameProcessors()
    }

    private fun getStack(err: Exception): String {
        val writer = StringWriter()

        err.printStackTrace(PrintWriter(writer))

        return writer.toString()
    }


}
