package dev.resonance.player.ui.common

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import dev.resonance.player.R

/**
 * Opens the system photo picker. [save] receives the picked image and reports success;
 * a short message is shown when the image could not be used. Returns the function that opens the picker.
 */
@Composable
fun rememberImagePicker(save: (Uri, (Boolean) -> Unit) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentSave by rememberUpdatedState(save)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            currentSave(uri) { ok ->
                if (!ok) Toast.makeText(context, R.string.image_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
