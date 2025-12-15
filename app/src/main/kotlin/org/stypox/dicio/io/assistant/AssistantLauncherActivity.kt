package org.stypox.dicio.io.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * A transparent activity that immediately launches the assistant overlay service
 * and finishes itself. This is used to handle ASSIST and VOICE_COMMAND intents.
 */
class AssistantLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Launch the overlay service
        AssistantOverlayService.start(this)
        
        // Finish this activity immediately
        finish()
    }
}
