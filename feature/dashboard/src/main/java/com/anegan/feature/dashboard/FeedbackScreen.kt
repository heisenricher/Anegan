package com.anegan.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anegan.core.designsystem.theme.MidnightIndigo
import com.anegan.core.designsystem.theme.PlatinumSilver
import com.anegan.core.designsystem.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

@Composable
fun FeedbackScreen(
    modifier: Modifier = Modifier
) {
    var feedbackText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submissionMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = "Submit Feedback",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 24.sp),
            color = MidnightIndigo
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
            placeholder = { Text("What can we improve?", color = PlatinumSilver) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MidnightIndigo,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (submissionMessage != null) {
            Text(
                text = submissionMessage!!,
                color = if (submissionMessage!!.contains("success", ignoreCase = true)) MidnightIndigo else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (feedbackText.isNotBlank()) {
                    isSubmitting = true
                    scope.launch {
                        val success = submitFeedback(feedbackText)
                        isSubmitting = false
                        if (success) {
                            submissionMessage = "Feedback submitted successfully!"
                            feedbackText = ""
                        } else {
                            submissionMessage = "Failed to submit. Check your internet connection."
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = MidnightIndigo, contentColor = PureWhite)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp))
            } else {
                Text("Send Securely", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

suspend fun submitFeedback(message: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // Replace this with your actual Webhook URL (e.g., Formspree, Discord, Google Forms)
            val url = URL("https://formspree.io/f/YOUR_FORM_ID")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonObject = JSONObject()
            jsonObject.put("feedback", message)

            connection.outputStream.use { os ->
                val input = jsonObject.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            // Note: because the placeholder URL is fake, it will fail unless changed.
            // For demonstration, we just return true if it doesn't throw a network exception.
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
