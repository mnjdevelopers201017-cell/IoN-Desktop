import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.awt.Desktop
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Base64
import javax.imageio.ImageIO

object AutomationEngine {
    private val robot = Robot()
    
    @Volatile var isRunning = false
    @Volatile private var stopRequested = false

    // EXACT Models from Google AI Studio, ordered by speed/quota
    private val modelFallbackChain = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash",
        "gemini-3-flash-preview",
        "gemini-3.6-flash",
        "gemini-3.8-flash",
        "gemini-1.5-flash" // Ultimate safety net
    )
    private var currentModelIndex = 0

    fun stopTask() {
        stopRequested = true
    }

    suspend fun runTask(task: String, apiKey: String, onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        isRunning = true
        stopRequested = false
        onLog("▶ Starting task: $task")
        var step = 1
        val maxSteps = 30
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        currentModelIndex = 0

        while (step <= maxSteps && !stopRequested) {
            onLog("Step $step: Capturing screen...")
            
            val image = robot.createScreenCapture(Rectangle(screenSize))
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "jpg", baos)
            val base64Image = Base64.getEncoder().encodeToString(baos.toByteArray())

            val jsonResponse = askGeminiWithRetry(base64Image, task, apiKey, onLog)
            
            if (stopRequested) break

            if (jsonResponse == null) {
                onLog("❌ Failed to get a response from all models. Retrying step...")
                Thread.sleep(3000)
                continue
            }

            val action = jsonResponse.optString("action")
            val reason = jsonResponse.optString("reason")
            onLog("Step $step: AI says -> $action ($reason)")

            if (action == "done") {
                onLog("✅ Task Complete!")
                break
            }
            if (action == "fail") {
                onLog("❌ AI failed: $reason")
                break
            }

            try {
                when (action) {
                    "open_url" -> {
                        val url = jsonResponse.getString("url")
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(URI(url))
                            onLog("   Opened browser: $url")
                            Thread.sleep(4000) // Wait for page to load
                        }
                    }
                    "tap" -> {
                        val x = (jsonResponse.getInt("x") / 1000.0 * screenSize.width).toInt()
                        val y = (jsonResponse.getInt("y") / 1000.0 * screenSize.height).toInt()
                        robot.mouseMove(x, y)
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                    }
                    "type" -> {
                        val text = jsonResponse.getString("text")
                        typeString(text)
                    }
                    "wait" -> {
                        val ms = jsonResponse.optInt("duration", 2000)
                        Thread.sleep(ms.toLong())
                    }
                }
            } catch (e: Exception) {
                onLog("Error executing action: ${e.message}")
            }

            step++
            Thread.sleep(1500) 
        }
        
        if (stopRequested) onLog("🛑 Automation stopped by user.")
        isRunning = false
    }

    private fun askGeminiWithRetry(base64Image: String, task: String, apiKey: String, onLog: (String) -> Unit): JSONObject? {
        var attempts = 0
        val maxAttempts = modelFallbackChain.size * 2

        while (attempts < maxAttempts && !stopRequested) {
            val model = modelFallbackChain[currentModelIndex]
            onLog("   Asking $model...")
            
            val (result, errorMsg) = askGemini(base64Image, task, apiKey, model)
            
            if (result != null && result.optString("_error") == "quota") {
                onLog("   ⚠️ Quota hit on $model. Switching to next model...")
                currentModelIndex = (currentModelIndex + 1) % modelFallbackChain.size
                attempts++
                Thread.sleep(1000)
                continue
            }

            if (result != null && result.optString("_error") == "503") {
                onLog("   ⚠️ Model overloaded (503). Waiting...")
                Thread.sleep(3000L * (attempts + 1)) // Exponential backoff
                attempts++
                continue
            }
            
            if (result != null) return result
            
            onLog("   ⚠️ Error on $model: $errorMsg")
            if (errorMsg?.contains("404") == true || errorMsg?.contains("400") == true) {
                onLog("   -> Skipping to next model...")
                currentModelIndex = (currentModelIndex + 1) % modelFallbackChain.size
            }
            
            Thread.sleep(2000)
            attempts++
        }
        return null
    }

    private fun askGemini(base64Image: String, task: String, apiKey: String, model: String): Pair<JSONObject?, String?> {
        val prompt = """
            You are a Desktop Automation Agent controlling a ${Toolkit.getDefaultToolkit().screenSize.width}x${Toolkit.getDefaultToolkit().screenSize.height} screen.
            TASK: $task
            Look at the current screen and decide the NEXT action.
            
            If the task requires a website, use "open_url" first.
            If you need to click a button, use "tap".
            If you need to type, use "type".
            If you need to wait for a page to load, use "wait".
            
            Reply ONLY in raw JSON. Choose one:
            Open URL: {"action":"open_url","url":"https://...","reason":"..."}
            Tap: {"action":"tap","x":<0-1000>,"y":<0-1000>,"reason":"..."}
            Type: {"action":"type","text":"<text>","reason":"..."}
            Wait: {"action":"wait","duration":<ms>,"reason":"..."}
            Done: {"action":"done","reason":"..."}
            Fail: {"action":"fail","reason":"..."}
        """.trimIndent()

        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().apply {
                put(JSONObject().put("text", prompt))
                put(JSONObject().put("inline_data", JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", base64Image)
                }))
            })))
        }.toString()

        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            if (code == 429) return Pair(JSONObject().put("_error", "quota"), null)
            if (code == 503) return Pair(JSONObject().put("_error", "503"), null)
            if (code != 200) {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                return Pair(null, "HTTP $code: ${errorBody.take(100)}")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val text = JSONObject(response).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            
            val clean = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val s = clean.indexOf('{')
            val e = clean.lastIndexOf('}')
            if (s < 0 || e < 0) return Pair(null, "Bad JSON response from AI")
            
            Pair(JSONObject(clean.substring(s, e + 1)), null)
        } catch (e: Exception) {
            Pair(null, e.message)
        }
    }

    private fun typeString(text: String) {
        for (char in text) {
            val keyCode = KeyEvent.getExtendedKeyCodeForChar(char.code)
            if (keyCode == KeyEvent.VK_UNDEFINED) continue 
            robot.keyPress(keyCode)
            robot.keyRelease(keyCode)
            Thread.sleep(50)
        }
    }
}
