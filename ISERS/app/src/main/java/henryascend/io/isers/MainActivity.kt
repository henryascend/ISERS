package henryascend.io.isers

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    fun goToRecognition(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, RecognitionActivity::class.java))
    }

    fun goToStreaming(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, StreamingActivity::class.java))
    }



}
