package com.plaquote.app

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.InputType
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

        // Header
        p.shader = LinearGradient(0f, 0f, w, h * 0.14f, blue, darkBlue, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h * 0.14f, p)
        p.shader = null

        // Theme toggle
        drawToggle(c, 24f, 26f, 168f, 50f)

        // Search button
        p.color = Color.argb(65, 255, 255, 255)
        c.drawCircle(w - 52f, 52f, 26f, p)
        drawSearchIcon(c, w - 52f, 52f)

        // FAB (+)
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

        // Theme toggle
        if (x in 24f..192f && y in 26f..76f) {
            toggleTheme()
        }
        // Search
        else if ((x - (w - 52f)).pow(2) + (y - 52f).pow(2) < 30f * 30f) {
            Toast.makeText(ctx, "Search — coming soon", Toast.LENGTH_SHORT).show()
        }
        // + button (FAB)
        else if ((x - (w - 56f)).pow(2) + (y - (h - 58f)).pow(2) < 36f * 36f) {
            showAddPatientDialog()
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

    // ==================== ADD PATIENT FORM ====================

    private fun showAddPatientDialog() {
        val activity = ctx as Activity

        // Main white card
        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 32)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 28f
            }
        }

        // Title
        val title = TextView(activity).apply {
            text = "New Patient"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1a1a1a"))
            setPadding(0, 0, 0, 28)
        }
        card.addView(title)

        // Helper to create labeled field
        fun labeledField(label: String, hint: String, inputType: Int = InputType.TYPE_CLASS_TEXT): EditText {
            val labelView = TextView(activity).apply {
                text = label
                textSize = 13f
                setTextColor(Color.parseColor("#555555"))
                setPadding(4, 12, 0, 6)
            }
            card.addView(labelView)

            val edit = EditText(activity).apply {
                this.hint = hint
                this.inputType = inputType
                setPadding(28, 22, 28, 22)
                textSize = 15f
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5F7FA"))
                    cornerRadius = 16f
                }
            }
            card.addView(edit)
            return edit
        }

        // Chamber (Spinner)
        val chamberLabel = TextView(activity).apply {
            text = "Chamber"
            textSize = 13f
            setTextColor(Color.parseColor("#555555"))
            setPadding(4, 12, 0, 6)
        }
        card.addView(chamberLabel)

        val chamberSpinner = Spinner(activity).apply {
            adapter = ArrayAdapter(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Mohanpur", "Ranirbazar")
            )
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F5F7FA"))
                cornerRadius = 16f
            }
        }
        card.addView(chamberSpinner)

        // Other fields
        val nameEdit  = labeledField("Patient's Name", "Enter full name")
        val ageEdit   = labeledField("Age", "e.g. 32", InputType.TYPE_CLASS_NUMBER)
        
        // Gender
        val genderLabel = TextView(activity).apply {
            text = "Gender"
            textSize = 13f
            setTextColor(Color.parseColor("#555555"))
            setPadding(4, 16, 0, 6)
        }
        card.addView(genderLabel)

        val genderSpinner = Spinner(activity).apply {
            adapter = ArrayAdapter(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Male", "Female", "Other")
            )
            setPadding(16, 12, 16, 12)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F5F7FA"))
                cornerRadius = 16f
            }
        }
        card.addView(genderSpinner)

        val phoneEdit = labeledField("Phone Number", "e.g. 9876543210", InputType.TYPE_CLASS_PHONE)

        // Buttons row
        val buttonRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 32, 0, 0)
        }

        val cancelBtn = Button(activity).apply {
            text = "Cancel"
            setTextColor(Color.parseColor("#666666"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EEEEEE"))
                cornerRadius = 16f
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 12
            }
        }

        val saveBtn = Button(activity).apply {
            text = "Save"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(blue)
                cornerRadius = 16f
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
        }

        buttonRow.addView(cancelBtn)
        buttonRow.addView(saveBtn)
        card.addView(buttonRow)

        // Dialog with dimmed background
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(card)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.90).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            // Dim / blur effect
            setDimAmount(0.55f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        cancelBtn.setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val chamber = chamberSpinner.selectedItem.toString()
            val name    = nameEdit.text.toString().trim()
            val age     = ageEdit.text.toString().trim()
            val gender  = genderSpinner.selectedItem.toString()
            val phone   = phoneEdit.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(activity, "Please enter patient's name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // For now just show confirmation (later we can save to database)
            Toast.makeText(
                activity,
                "Saved:\n$chamber • $name • $age yrs • $gender • $phone",
                Toast.LENGTH_LONG
            ).show()

            dialog.dismiss()
        }

        dialog.show()
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
