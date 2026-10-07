package com.plaquote.app

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var updater: AppUpdater

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(PlaquoteView(this))
        updater = AppUpdater(this)
        updater.check(silent = true)
    }

    override fun onDestroy() {
        if (::updater.isInitialized) updater.cancelled = true
        super.onDestroy()
    }
}

class PlaquoteView(private val ctx: Context) : View(ctx) {

    private var themeProgress = 0f
    private var targetDark = false
    private var animator: ValueAnimator? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val toothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.8f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val lightTooth = Color.rgb(160, 200, 240)
    private val darkTooth  = Color.rgb(210, 230, 255)
    private val blue       = Color.rgb(22, 119, 240)
    private val darkBlue   = Color.rgb(7, 82, 199)

    private data class ToothSpec(
        val nx: Float, val ny: Float, val scale: Float,
        val sparkle: Boolean = false, val shine: Boolean = false
    )

    private val teeth = listOf(
        ToothSpec(0.10f, 0.28f, 0.95f, sparkle = true),
        ToothSpec(0.28f, 0.22f, 0.75f),
        ToothSpec(0.48f, 0.30f, 1.10f, sparkle = true, shine = true),
        ToothSpec(0.72f, 0.25f, 0.85f),
        ToothSpec(0.90f, 0.32f, 0.70f, sparkle = true),
        ToothSpec(0.15f, 0.48f, 0.80f, shine = true),
        ToothSpec(0.38f, 0.52f, 0.95f),
        ToothSpec(0.58f, 0.45f, 0.70f, sparkle = true),
        ToothSpec(0.80f, 0.50f, 1.05f, sparkle = true),
        ToothSpec(0.08f, 0.68f, 0.85f),
        ToothSpec(0.32f, 0.72f, 0.75f, sparkle = true),
        ToothSpec(0.55f, 0.65f, 1.00f, shine = true),
        ToothSpec(0.78f, 0.70f, 0.80f),
        ToothSpec(0.22f, 0.88f, 0.70f),
        ToothSpec(0.48f, 0.85f, 0.90f, sparkle = true),
        ToothSpec(0.70f, 0.90f, 0.75f),
        ToothSpec(0.92f, 0.78f, 0.85f, sparkle = true)
    )

    private val sparkles = listOf(
        0.05f to 0.20f, 0.18f to 0.35f, 0.35f to 0.18f,
        0.55f to 0.38f, 0.68f to 0.15f, 0.85f to 0.42f,
        0.12f to 0.58f, 0.42f to 0.62f, 0.62f to 0.55f,
        0.88f to 0.60f, 0.25f to 0.80f, 0.50f to 0.78f,
        0.75f to 0.85f, 0.08f to 0.92f, 0.95f to 0.25f
    )

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        val bg = lerpColor(Color.WHITE, Color.rgb(11, 18, 32), themeProgress)
        c.drawColor(bg)

        val toothColor = lerpColor(lightTooth, darkTooth, themeProgress)
        toothPaint.color = toothColor
        p.color = toothColor

        for ((nx, ny) in sparkles) {
            drawSparkle(c, nx * w, ny * h, 6f + (nx * 5f), toothColor)
        }

        for (t in teeth) {
            drawMolar(c, t.nx * w, t.ny * h, 28f * t.scale, t.sparkle, t.shine)
        }

        p.shader = LinearGradient(0f, 0f, w, h * 0.14f, blue, darkBlue, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h * 0.14f, p)
        p.shader = null

        drawToggle(c, 24f, 26f, 168f, 50f)

        p.color = Color.argb(65, 255, 255, 255)
        c.drawCircle(w - 52f, 52f, 26f, p)
        drawSearchIcon(c, w - 52f, 52f)

        drawAI(c, 20f, h - 74f)

        p.setShadowLayer(16f, 0f, 6f, Color.argb(70, 0, 70, 180))
        setLayerType(LAYER_TYPE_SOFTWARE, p)
        p.color = blue
        c.drawCircle(w - 56f, h - 58f, 30f, p)
        p.clearShadowLayer()
        p.color = Color.WHITE
        p.strokeWidth = 4.5f
        p.strokeCap = Paint.Cap.ROUND
        c.drawLine(w - 70f, h - 58f, w - 42f, h - 58f, p)
        c.drawLine(w - 56f, h - 72f, w - 56f, h - 44f, p)
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun drawMolar(c: Canvas, x: Float, y: Float, s: Float, sparkle: Boolean, shine: Boolean) {
        val path = Path()
        path.moveTo(x - s * 0.42f, y - s * 0.05f)
        path.cubicTo(x - s * 0.48f, y - s * 0.45f, x - s * 0.22f, y - s * 0.62f, x, y - s * 0.55f)
        path.cubicTo(x + s * 0.22f, y - s * 0.62f, x + s * 0.48f, y - s * 0.45f, x + s * 0.42f, y - s * 0.05f)
        path.cubicTo(x + s * 0.38f, y + s * 0.15f, x + s * 0.28f, y + s * 0.35f, x + s * 0.18f, y + s * 0.58f)
        path.cubicTo(x + s * 0.12f, y + s * 0.72f, x + s * 0.02f, y + s * 0.70f, x, y + s * 0.55f)
        path.cubicTo(x - s * 0.02f, y + s * 0.70f, x - s * 0.12f, y + s * 0.72f, x - s * 0.18f, y + s * 0.58f)
        path.cubicTo(x - s * 0.28f, y + s * 0.35f, x - s * 0.38f, y + s * 0.15f, x - s * 0.42f, y - s * 0.05f)
        path.close()
        c.drawPath(path, toothPaint)

        if (shine) {
            p.strokeWidth = 2f
            p.color = toothPaint.color
            c.drawLine(x + s * 0.22f, y - s * 0.25f, x + s * 0.38f, y - s * 0.18f, p)
            c.drawLine(x + s * 0.25f, y - s * 0.10f, x + s * 0.40f, y - s * 0.05f, p)
        }
        if (sparkle) {
            drawSparkle(c, x + s * 0.28f, y - s * 0.48f, s * 0.22f, toothPaint.color)
        }
    }

    private fun drawSparkle(c: Canvas, x: Float, y: Float, size: Float, color: Int) {
        p.color = color
        p.strokeWidth = max(1.5f, size * 0.22f)
        p.strokeCap = Paint.Cap.ROUND
        c.drawLine(x, y - size, x, y + size, p)
        c.drawLine(x - size, y, x + size, y, p)
        val d = size * 0.55f
        c.drawLine(x - d, y - d, x + d, y + d, p)
        c.drawLine(x + d, y - d, x - d, y + d, p)
        p.strokeCap = Paint.Cap.BUTT
    }

    private fun drawToggle(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        p.color = Color.argb(190, 0, 50, 130)
        c.drawRoundRect(x, y, x + w, y + h, h / 2, h / 2, p)

        val knobX = x + h / 2 + (w - h) * themeProgress
        p.color = Color.WHITE
        c.drawCircle(knobX, y + h / 2, h / 2 - 5f, p)

        p.color = if (themeProgress > 0.5f) Color.rgb(210, 225, 250) else blue
        p.textSize = 22f
        p.textAlign = Paint.Align.CENTER
        c.drawText(if (themeProgress > 0.5f) "☾" else "☀", knobX, y + h / 2 + 7f, p)
        p.textAlign = Paint.Align.LEFT
    }

    private fun drawSearchIcon(c: Canvas, x: Float, y: Float) {
        p.color = Color.WHITE
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3.5f
        c.drawCircle(x - 3f, y - 3f, 9f, p)
        c.drawLine(x + 4f, y + 4f, x + 12f, y + 12f, p)
        p.style = Paint.Style.FILL
    }

    private fun drawAI(c: Canvas, x: Float, y: Float) {
        p.color = blue
        c.drawRoundRect(x, y, x + 148f, y + 48f, 24f, 24f, p)
        p.color = Color.WHITE
        c.drawRoundRect(x + 12f, y + 12f, x + 48f, y + 36f, 8f, 8f, p)
        p.color = blue
        c.drawCircle(x + 24f, y + 24f, 3f, p)
        c.drawCircle(x + 36f, y + 24f, 3f, p)
        p.strokeWidth = 2f
        c.drawLine(x + 25f, y + 31f, x + 35f, y + 31f, p)
        p.color = Color.WHITE
        p.textSize = 17f
        p.typeface = Typeface.DEFAULT_BOLD
        c.drawText("AI Help", x + 58f, y + 30f, p)
        p.typeface = Typeface.DEFAULT
    }

    private fun toggleTheme() {
        targetDark = !targetDark
        animator?.cancel()
        animator = ValueAnimator.ofFloat(themeProgress, if (targetDark) 1f else 0f).apply {
            duration = 280
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                themeProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        val x = e.x
        val y = e.y
        val w = width.toFloat()
        val h = height.toFloat()

        if (x in 24f..192f && y in 26f..76f) {
            toggleTheme()
        } else if ((x - (w - 52f)).pow(2) + (y - 52f).pow(2) < 30f * 30f) {
            Toast.makeText(ctx, "Search — coming soon", Toast.LENGTH_SHORT).show()
        } else if (x in 20f..168f && y > h - 90f) {
            showAIHelpDialog()
        }
        return true
    }

    private fun lerpColor(a: Int, b: Int, t: Float): Int {
        val t2 = t.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(a)   + (Color.red(b)   - Color.red(a))   * t2).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * t2).toInt(),
            (Color.blue(a)  + (Color.blue(b)  - Color.blue(a))  * t2).toInt()
        )
    }

    // ==================== AI HELP ====================

    private fun showAIHelpDialog() {
        val activity = ctx as Activity
        val prefs = activity.getSharedPreferences("plaquote_prefs", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("gemini_api_key", null)

        if (savedKey.isNullOrBlank()) {
            askForApiKey(activity, prefs)
        } else {
            openChatDialog(activity, savedKey)
        }
    }

    private fun askForApiKey(activity: Activity, prefs: SharedPreferences) {
        val input = EditText(activity).apply {
            hint = "Paste your Gemini API key here"
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(activity)
            .setTitle("Gemini API Key Required")
            .setMessage("Get a free key at:\nhttps://aistudio.google.com/app/apikey\n\nIt will be saved only on this device.")
            .setView(input)
            .setPositiveButton("Save & Continue") { _, _ ->
                val key = input.text.toString().trim()
                if (key.startsWith("AIza")) {
                    prefs.edit().putString("gemini_api_key", key).apply()
                    openChatDialog(activity, key)
                } else {
                    Toast.makeText(activity, "Invalid API key", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openChatDialog(activity: Activity, apiKey: String) {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
        }

        val chatView = TextView(activity).apply {
            text = "Ask any dental question...\n\n"
            textSize = 15f
            setTextColor(Color.parseColor("#222222"))
            setPadding(0, 0, 0, 16)
        }

        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 480
            )
            addView(chatView)
        }

        val input = EditText(activity).apply {
            hint = "Type your question..."
            setPadding(28, 22, 28, 22)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0F4F8"))
                cornerRadius = 18f
            }
        }

        val sendBtn = Button(activity).apply {
            text = "Ask AI"
            setBackgroundColor(Color.parseColor("#1677F0"))
            setTextColor(Color.WHITE)

            setOnClickListener {
                val question = input.text.toString().trim()
                if (question.isEmpty()) return@setOnClickListener

                chatView.append("You: $question\n\n")
                chatView.append("AI: Thinking...\n\n")
                input.setText("")
                scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                isEnabled = false

                thread {
                    val answer = askGemini(question, apiKey)
                    activity.runOnUiThread {
                        val current = chatView.text.toString()
                        chatView.text = current.replace("AI: Thinking...\n\n", "AI: $answer\n\n")
                        isEnabled = true
                        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                    }
                }
            }
        }

        val changeKey = TextView(activity).apply {
            text = "Change API Key"
            setTextColor(Color.parseColor("#1677F0"))
            textSize = 13f
            setPadding(0, 18, 0, 0)
            setOnClickListener {
                activity.getSharedPreferences("plaquote_prefs", Context.MODE_PRIVATE)
                    .edit().remove("gemini_api_key").apply()
                Toast.makeText(activity, "API key cleared. Open AI Help again.", Toast.LENGTH_LONG).show()
            }
        }

        container.addView(scroll)
        container.addView(input)
        container.addView(sendBtn)
        container.addView(changeKey)

        AlertDialog.Builder(activity)
            .setTitle("Dental AI Help")
            .setView(container)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun askGemini(question: String, apiKey: String): String {
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 25000

            val prompt = """
                You are a helpful and accurate dental assistant.
                Answer clearly and professionally.
                Focus only on dental and oral health topics.
                Always end with this disclaimer:
                "⚠️ This is general information only and not a substitute for professional dental advice."
                
                User question: $question
            """.trimIndent()

            val body = """
                {
                  "contents": [{
                    "parts": [{"text": ${JSONObject.quote(prompt)}}]
                  }]
                }
            """.trimIndent()

            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            val response = if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $code"
            }
            conn.disconnect()

            if (code !in 200..299) {
                return "Sorry, AI is temporarily unavailable (Error $code).\nCheck your API key."
            }

            val json = JSONObject(response)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val parts = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "No answer received.")
                }
            }
            "Sorry, I couldn't generate an answer."
        } catch (e: Exception) {
            "Connection error. Check your internet.\n(${e.message})"
        }
    }
}

// ==================== UPDATER ====================

class AppUpdater(private val activity: Activity) {
    companion object {
        const val API = "https://api.github.com/repos/kawai-uhq/plaquote/releases/latest"
    }

    @Volatile var cancelled = false

    private fun currentVersion(): String {
        return try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0"
        } catch (_: Exception) {
            "0"
        }
    }

    fun check(silent: Boolean) {
        thread {
            try {
                val conn = URL(API).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "Plaquote-App")

                if (conn.responseCode != 200) {
                    conn.disconnect()
                    return@thread
                }

                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                conn.disconnect()

                val remote = json.optString("tag_name").removePrefix("v")
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").equals("Plaquote.apk", true)) {
                            apkUrl = a.optString("browser_download_url")
                            break
                        }
                    }
                }

                if (!cancelled && apkUrl != null && isNewer(remote, currentVersion())) {
                    activity.runOnUiThread { showDialog(remote, apkUrl!!) }
                }
            } catch (_: Exception) {}
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun showDialog(version: String, url: String) {
        AlertDialog.Builder(activity)
            .setTitle("Plaquote update available")
            .setMessage("Version $version is available.\nDownload and install it now?")
            .setNegativeButton("Later", null)
            .setPositiveButton("Update") { _, _ -> download(url, version) }
            .show()
    }

    private fun download(url: String, version: String) {
        thread {
            try {
                val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
                val file = File(dir, "Plaquote-$version.apk")
                URL(url).openStream().use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                if (!cancelled) activity.runOnUiThread { install(file) }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Update download failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun install(file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivity(intent)
            Toast.makeText(activity, "Allow Plaquote to install updates, then open the app again.", Toast.LENGTH_LONG).show()
            return
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}
