import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.awt.Desktop
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Base64
import javax.imageio.ImageIO

object AutomationEngine {
    private var robot: Robot? = null
    
    @Volatile var isRunning = false
    @Volatile private var stopRequested = false

    private val modelFallbackChain = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash",
        "gemini-3-flash",
        "gemini-3.6-flash",
        "gemini-3.8-flash"
    )
    private var currentModelIndex = 0

    fun stopTask() { stopRequested = true }

    fun testMouseControl(): String {
        return try {
            val r = Robot()
            val originalPos = java.awt.MouseInfo.getPointerInfo().location
            r.mouseMove(originalPos.x + 100, originalPos.y + 100)
            Thread.sleep(200)
            val newPos = java.awt.MouseInfo.getPointerInfo().location
            r.mouseMove(originalPos.x, originalPos.y)
            
            if (originalPos != newPos) "✅ Mouse control works!" else "❌ Mouse control failed (OS blocked it)"
        } catch (e: Exception) {
            "❌ Error: ${e.message}"
        }
    }

    suspend fun runTask(task: String, apiKey: String, onLog: (String) -> Unit) = withContext(Dispatchers.IO) {
        isRunning = true
        stopRequested = false
        onLog("Waiting 3 seconds before starting...")
        Thread.sleep(3000) 

        var step = 1
        val maxSteps = 40
        currentModelIndex = 0
        
        var lastAction = ""
        var consecutiveActionCount = 0

        try {
            if (robot == null) robot = Robot()
            val screenSize = Toolkit.getDefaultToolkit().screenSize

            while (step <= maxSteps && !stopRequested) {
                onLog("Step $step: Capturing screen...")
                
                val image = try {
                    robot!!.createScreenCapture(Rectangle(screenSize))
                } catch (e: Exception) {
                    onLog("❌ Failed to capture screen: ${e.message}")
                    break
                }

                val baos = ByteArrayOutputStream()
                ImageIO.write(image, "jpg", baos)
                val base64Image = Base64.getEncoder().encodeToString(baos.toByteArray())

                val jsonResponse = askGeminiWithRetry(base64Image, task, apiKey, onLog)
                
                if (stopRequested) break
                if (jsonResponse == null) {
                    onLog("❌ Failed to get a response. Retrying...")
                    Thread.sleep(3000)
                    continue
                }

                val action = jsonResponse.optString("action")
                val reason = jsonResponse.optString("reason")
                onLog("Step $step: AI says -> $action ($reason)")

                if (action == "done") { onLog("✅ Task Complete!"); break }
                if (action == "fail") { onLog("❌ AI failed: $reason"); break }

                if (action == lastAction) {
                    consecutiveActionCount++
                    if (consecutiveActionCount >= 3) {
                        onLog("⚠️ AI stuck in loop. Forcing a 3-second wait...")
                        Thread.sleep(3000)
                        consecutiveActionCount = 0
                        lastAction = "wait"
                        step++
                        continue
                    }
                } else {
                    lastAction = action
                    consecutiveActionCount = 1
                }

                try {
                    val screenWidth = screenSize.width
                    val screenHeight = screenSize.height

                    when (action) {
                        "open_url" -> {
                            val url = jsonResponse.getString("url")
                            onLog("   Opening browser: $url")
                            try {
                                ProcessBuilder("firefox-esr", url).start()
                            } catch (e: Exception) {
                                try { ProcessBuilder("xdg-open", url).start() } catch (e2: Exception) {}
                            }
                            Thread.sleep(5000)
                        }
                        "left_click" -> {
                            val x = (jsonResponse.getInt("x") / 1000.0 * screenWidth).toInt()
                            val y = (jsonResponse.getInt("y") / 1000.0 * screenHeight).toInt()
                            robot!!.mouseMove(x, y)
                            robot!!.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                            robot!!.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                            Thread.sleep(500)
                        }
                        "right_click" -> {
                            val x = (jsonResponse.getInt("x") / 1000.0 * screenWidth).toInt()
                            val y = (jsonResponse.getInt("y") / 1000.0 * screenHeight).toInt()
                            robot!!.mouseMove(x, y)
                            robot!!.mousePress(InputEvent.BUTTON3_DOWN_MASK)
                            robot!!.mouseRelease(InputEvent.BUTTON3_DOWN_MASK)
                            Thread.sleep(500)
                        }
                        "double_click" -> {
                            val x = (jsonResponse.getInt("x") / 1000.0 * screenWidth).toInt()
                            val y = (jsonResponse.getInt("y") / 1000.0 * screenHeight).toInt()
                            robot!!.mouseMove(x, y)
                            robot!!.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                            robot!!.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                            Thread.sleep(50)
                            robot!!.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                            robot!!.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                            Thread.sleep(500)
                        }
                        "drag" -> {
                            val x1 = (jsonResponse.getInt("x1") / 1000.0 * screenWidth).toInt()
                            val y1 = (jsonResponse.getInt("y1") / 1000.0 * screenHeight).toInt()
                            val x2 = (jsonResponse.getInt("x2") / 1000.0 * screenWidth).toInt()
                            val y2 = (jsonResponse.getInt("y2") / 1000.0 * screenHeight).toInt()
                            robot!!.mouseMove(x1, y1)
                            robot!!.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                            Thread.sleep(500)
                            robot!!.mouseMove(x2, y2)
                            Thread.sleep(500)
                            robot!!.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                            Thread.sleep(500)
                        }
                        "scroll" -> {
                            val amount = jsonResponse.optInt("amount", -3)
                            robot!!.mouseWheel(amount) 
                            Thread.sleep(500)
                        }
                        "paste" -> {
                            // NEW: Instant Clipboard Pasting
                            val text = jsonResponse.getString("text")
                            val stringSelection = StringSelection(text)
                            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                            clipboard.setContents(stringSelection, stringSelection)
                            
                            // Simulate Ctrl+V
                            robot!!.keyPress(KeyEvent.VK_CONTROL)
                            robot!!.keyPress(KeyEvent.VK_V)
                            Thread.sleep(100)
                            robot!!.keyRelease(KeyEvent.VK_V)
                            robot!!.keyRelease(KeyEvent.VK_CONTROL)
                            onLog("   Pasted text instantly.")
                            Thread.sleep(500)
                        }
                        "press_key" -> {
                            val keyName = jsonResponse.getString("key").uppercase()
                            pressKey(keyName)
                            Thread.sleep(500)
                        }
                        "hotkey" -> {
                            val keysArray = jsonResponse.getJSONArray("keys")
                            val keys = mutableListOf<String>()
                            for (i in 0 until keysArray.length()) keys.add(keysArray.getString(i).uppercase())
                            pressHotkey(keys)
                            Thread.sleep(500)
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
            }
        } catch (e: Exception) {
            onLog("❌ FATAL ERROR: ${e.message}")
        }
        
        if (stopRequested) onLog("🛑 Automation stopped by user.")
        isRunning = false
    }

    private fun pressKey(key: String) {
        val code = when (key) {
            "ENTER" -> KeyEvent.VK_ENTER
            "TAB" -> KeyEvent.VK_TAB
            "ESC", "ESCAPE" -> KeyEvent.VK_ESCAPE
            "BACKSPACE" -> KeyEvent.VK_BACK_SPACE
            "SPACE" -> KeyEvent.VK_SPACE
            "UP" -> KeyEvent.VK_UP
            "DOWN" -> KeyEvent.VK_DOWN
            "LEFT" -> KeyEvent.VK_LEFT
            "RIGHT" -> KeyEvent.VK_RIGHT
            else -> KeyEvent.VK_UNDEFINED
        }
        if (code != KeyEvent.VK_UNDEFINED) {
            robot!!.keyPress(code)
            robot!!.keyRelease(code)
        }
    }

    private fun pressHotkey(keys: List<String>) {
        val codes = keys.mapNotNull {
            when (it) {
                "CTRL", "CONTROL" -> KeyEvent.VK_CONTROL
                "ALT" -> KeyEvent.VK_ALT
                "SHIFT" -> KeyEvent.VK_SHIFT
                "A" -> KeyEvent.VK_A
                "C" -> KeyEvent.VK_C
                "V" -> KeyEvent.VK_V
                "T" -> KeyEvent.VK_T
                "W" -> KeyEvent.VK_W
                "TAB" -> KeyEvent.VK_TAB
                else -> null
            }
        }
        for (code in codes) robot!!.keyPress(code)
        Thread.sleep(100)
        for (code in codes.reversed()) robot!!.keyRelease(code)
    }

    private fun askGeminiWithRetry(base64Image: String, task: String, apiKey: String, onLog: (String) -> Unit): JSONObject? {
        var attempts = 0
        val maxAttempts = modelFallbackChain.size * 2
        while (attempts < maxAttempts && !stopRequested) {
            val model = modelFallbackChain[currentModelIndex]
            onLog("   Asking $model...")
            val (result, errorMsg) = askGemini(base64Image, task, apiKey, model)
            if (result != null && !result.has("_error")) return result
            onLog("   ⚠️ Error on $model: ${errorMsg ?: result?.optString("_error")}")
            currentModelIndex = (currentModelIndex + 1) % modelFallbackChain.size
            attempts++
            Thread.sleep(1500)
        }
        return null
    }

    private fun askGemini(base64Image: String, task: String, apiKey: String, model: String): Pair<JSONObject?, String?> {
        val prompt = """
            You are an Elite Desktop Automation Agent controlling a ${Toolkit.getDefaultToolkit().screenSize.width}x${Toolkit.getDefaultToolkit().screenSize.height} screen.
            TASK: $task
            Look at the current screen and decide the EXACT NEXT action to achieve the task.
            
            RULES:
            - Coordinates must be between 0 and 1000 (0 is left/top, 1000 is right/bottom).
            - Use "open_url" to launch websites.
            - USE "paste" INSTEAD OF TYPING FOR TEXT. It is instantly copied to the clipboard and pasted.
            - Use "hotkey" for Ctrl+C, Ctrl+V, Alt+Tab, etc. (e.g., ["CTRL", "V"]).
            - Use "press_key" for Enter, Tab, Esc, etc.
            - Use "scroll" with a negative amount to scroll down, positive to scroll up.
            - Use "drag" to highlight text or move sliders.
            
            Reply ONLY in raw JSON. Choose ONE action from this list:
            
            1. Open URL: {"action":"open_url","url":"https://...","reason":"..."}
            2. Left Click: {"action":"left_click","x":<0-1000>,"y":<0-1000>,"reason":"..."}
            3. Right Click: {"action":"right_click","x":<0-1000>,"y":<0-1000>,"reason":"..."}
            4. Double Click: {"action":"double_click","x":<0-1000>,"y":<0-1000>,"reason":"..."}
            5. Drag/Swipe: {"action":"drag","x1":<0-1000>,"y1":<0-1000>,"x2":<0-1000>,"y2":<0-1000>,"reason":"..."}
            6. Scroll: {"action":"scroll","amount":<int>,"reason":"..."} (negative = down, positive = up)
            7. Paste Text (USE THIS INSTEAD OF TYPING): {"action":"paste","text":"<text>","reason":"..."}
            8. Press Key: {"action":"press_key","key":"<ENTER|TAB|ESC|BACKSPACE>","reason":"..."}
            9. Hotkey: {"action":"hotkey","keys":["CTRL","V"],"reason":"..."}
            10. Wait: {"action":"wait","duration":<ms>,"reason":"..."}
            11. Done: {"action":"done","reason":"..."}
            12. Fail: {"action":"fail","reason":"..."}
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
            if (code != 200) return Pair(null, "HTTP $code")

            val response = conn.inputStream.bufferedReader().readText()
            val text = JSONObject(response).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            
            val clean = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val s = clean.indexOf('{')
            val e = clean.lastIndexOf('}')
            if (s < 0 || e < 0) return Pair(null, "Bad JSON")
            
            Pair(JSONObject(clean.substring(s, e + 1)), null)
        } catch (e: Exception) {
            Pair(null, e.message)
        }
    }
}
