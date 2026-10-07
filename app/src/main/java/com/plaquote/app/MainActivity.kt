package com.plaquote.app

import android.widget.*
import android.graphics.drawable.GradientDrawable
import org.json.JSONArray
import android.app.*
import android.os.*
import android.graphics.*
import android.view.*
import android.content.*
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.*
import java.net.*
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var updater: AppUpdater
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(PlaquoteView(this))
        updater = AppUpdater(this)
        updater.check(true)
    }
    override fun onDestroy() {
        if (::updater.isInitialized) updater.cancelled = true
        super.onDestroy()
    }
}

class PlaquoteView(private val ctx: Context) : View(ctx) {
    private val blue = Color.rgb(22,119,240)
    private val darkBlue = Color.rgb(7,82,199)
    private var dark = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tooth = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style=Paint.Style.STROKE
        strokeWidth=3f
        color=Color.rgb(225,238,253)
    }
    override fun onDraw(c: Canvas) {
        val w=width.toFloat(); val h=height.toFloat()
        c.drawColor(if(dark) Color.rgb(11,18,32) else Color.WHITE)

        // Tooth doodles similar to the requested reference.
        val xs=floatArrayOf(.07f,.24f,.43f,.64f,.84f,.16f,.35f,.55f,.75f,.94f,.05f,.28f,.49f,.70f,.88f)
        val ys=floatArrayOf(.31f,.39f,.29f,.43f,.34f,.57f,.51f,.61f,.54f,.65f,.78f,.70f,.82f,.73f,.88f)
        for(i in xs.indices) {
            drawTooth(c,xs[i]*w,ys[i]*h,25f+(i%3)*7f)
            p.color=Color.rgb(232,242,253)
            c.drawCircle(xs[i]*w+35f,ys[i]*h+15f,4.5f,p)
            if(i%3==0) {
                p.color=Color.rgb(218,236,253); p.strokeWidth=2.5f
                c.drawLine(xs[i]*w+45f,ys[i]*h-3f,xs[i]*w+57f,ys[i]*h-3f,p)
                c.drawLine(xs[i]*w+51f,ys[i]*h-9f,xs[i]*w+51f,ys[i]*h+3f,p)
            }
        }

        // Straight blue overhead overlay.
        p.shader=LinearGradient(0f,0f,w,h*.15f,blue,darkBlue,Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,w,h*.15f,p); p.shader=null

        drawToggle(c,24f,28f,174f,54f)
        p.color=Color.argb(65,255,255,255); c.drawCircle(w-54f,55f,28f,p)
        drawSearch(c,w-54f,55f)

        drawAI(c,24f,h-78f)

        p.setShadowLayer(18f,0f,7f,Color.argb(80,0,85,200))
        setLayerType(LAYER_TYPE_SOFTWARE,p); p.color=blue
        c.drawCircle(w-58f,h-60f,31f,p); p.clearShadowLayer()
        p.color=Color.WHITE; p.strokeWidth=5f; p.strokeCap=Paint.Cap.ROUND
        c.drawLine(w-72f,h-60f,w-44f,h-60f,p)
        c.drawLine(w-58f,h-74f,w-58f,h-46f,p)
        p.strokeCap=Paint.Cap.BUTT
    }

    private fun drawTooth(c: Canvas, x: Float, y: Float, s: Float) {
    val q = Path()

    // Upper crown — rounded and wide like a real tooth
    q.moveTo(
        x - s * 0.34f,
        y - s * 0.30f
    )

    q.cubicTo(
        x - s * 0.48f, y - s * 0.24f,
        x - s * 0.50f, y - s * 0.05f,
        x - s * 0.40f, y + s * 0.08f
    )

    // Left side narrowing toward the root
    q.cubicTo(
        x - s * 0.34f, y + s * 0.17f,
        x - s * 0.25f, y + s * 0.18f,
        x - s * 0.20f, y + s * 0.28f
    )

    // Left root
    q.cubicTo(
        x - s * 0.18f, y + s * 0.43f,
        x - s * 0.10f, y + s * 0.52f,
        x - s * 0.04f, y + s * 0.55f
    )

    q.cubicTo(
        x - s * 0.01f, y + s * 0.56f,
        x + s * 0.01f, y + s * 0.48f,
        x + s * 0.03f, y + s * 0.36f
    )

    // Right root
    q.cubicTo(
        x + s * 0.05f, y + s * 0.49f,
        x + s * 0.10f, y + s * 0.55f,
        x + s * 0.15f, y + s * 0.48f
    )

    q.cubicTo(
        x + s * 0.23f, y + s * 0.36f,
        x + s * 0.27f, y + s * 0.25f,
        x + s * 0.32f, y + s * 0.15f
    )

    // Right side of crown
    q.cubicTo(
        x + s * 0.43f, y - s * 0.01f,
        x + s * 0.47f, y - s * 0.20f,
        x + s * 0.34f, y - s * 0.30f
    )

    q.cubicTo(
        x + s * 0.25f, y - s * 0.37f,
        x + s * 0.15f, y - s * 0.30f,
        x, y - s * 0.24f
    )

    q.cubicTo(
        x - s * 0.15f, y - s * 0.30f,
        x - s * 0.25f, y - s * 0.37f,
        x - s * 0.34f, y - s * 0.30f
    )

    q.close()

    c.drawPath(q, tooth)

    // Small curved separation between crown and roots
    val detail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = s * 0.025f
        color = tooth.color
        strokeCap = Paint.Cap.ROUND
    }

    val crownLine = Path()

    crownLine.moveTo(
        x - s * 0.25f,
        y + s * 0.08f
    )

    crownLine.cubicTo(
        x - s * 0.12f,
        y + s * 0.16f,
        x + s * 0.12f,
        y + s * 0.16f,
        x + s * 0.25f,
        y + s * 0.08f
    )

    c.drawPath(crownLine, detail)
}

    private fun drawToggle(c:Canvas,x:Float,y:Float,w:Float,h:Float) {
        p.color=Color.argb(190,0,50,130); c.drawRoundRect(x,y,x+w,y+h,h/2,h/2,p)
        val k=if(dark)x+w-h/2 else x+h/2
        p.color=Color.WHITE; c.drawCircle(k,y+h/2,h/2-5f,p)
        p.color=if(dark)Color.rgb(210,225,250) else blue
        p.textSize=24f; p.textAlign=Paint.Align.CENTER
        c.drawText(if(dark)"☾" else "☀",k,y+h/2+8f,p); p.textAlign=Paint.Align.LEFT
    }

    private fun drawSearch(c:Canvas,x:Float,y:Float) {
        p.color=Color.WHITE; p.style=Paint.Style.STROKE; p.strokeWidth=4f
        c.drawCircle(x-3f,y-3f,9f,p); c.drawLine(x+4f,y+4f,x+12f,y+12f,p)
        p.style=Paint.Style.FILL
    }

    private fun drawAI(c:Canvas,x:Float,y:Float) {
        p.color=blue; c.drawRoundRect(x,y,x+152f,y+52f,26f,26f,p)
        p.color=Color.WHITE; c.drawRoundRect(x+15f,y+15f,x+52f,y+40f,8f,8f,p)
        p.color=blue; c.drawCircle(x+26f,y+27f,3f,p); c.drawCircle(x+41f,y+27f,3f,p)
        p.strokeWidth=2f; c.drawLine(x+27f,y+35f,x+40f,y+35f,p)
        p.color=Color.WHITE; p.textSize=18f; p.typeface=Typeface.DEFAULT_BOLD
        c.drawText("AI Help",x+64f,y+33f,p); p.typeface=Typeface.DEFAULT
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
        android.widget.Toast.makeText(ctx, "Search — coming soon", android.widget.Toast.LENGTH_SHORT).show()
    } else if (x in 20f..168f && y > h - 90f) {
        showAIHelpDialog()
    }
    return true
}

private fun showAIHelpDialog() {
    val activity = ctx as Activity

    val container = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(40, 30, 40, 20)
    }

    val chatView = TextView(ctx).apply {
        text = "Ask any dental question...\n\n"
        textSize = 15f
        setTextColor(Color.parseColor("#222222"))
        setPadding(0, 0, 0, 16)
    }

    val scroll = ScrollView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 480
        )
        addView(chatView)
    }

    val input = EditText(ctx).apply {
        hint = "Type your question here..."
        setPadding(28, 22, 28, 22)
        background = GradientDrawable().apply {
            setColor(Color.parseColor("#F0F4F8"))
            cornerRadius = 18f
        }
    }

    val sendBtn = Button(ctx).apply {
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
            sendBtn.isEnabled = false

            // Call real Gemini AI
            thread {
                val answer = askGemini(question)
                activity.runOnUiThread {
                    // Remove the "Thinking..." line
                    val current = chatView.text.toString()
                    chatView.text = current.replace("AI: Thinking...\n\n", "AI: $answer\n\n")
                    sendBtn.isEnabled = true
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }
    }

    container.addView(scroll)
    container.addView(input)
    container.addView(sendBtn)

    AlertDialog.Builder(activity)
        .setTitle("Dental AI Help")
        .setView(container)
        .setNegativeButton("Close", null)
        .show()
}

private fun askGemini(question: String): String {
    // ⚠️ Paste your Gemini API key here
    val apiKey = "PASTE_YOUR_GEMINI_API_KEY_HERE"

    if (apiKey == "PASTE_YOUR_GEMINI_API_KEY_HERE" || apiKey.isBlank()) {
        return "Please add your Gemini API key in the code first.\n\nGet a free key at:\nhttps://aistudio.google.com/app/apikey"
    }

    return try {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 20000

        val prompt = """
            You are a helpful and accurate dental assistant.
            Answer the user's question clearly and professionally.
            Focus only on dental and oral health topics.
            Always end your answer with this exact disclaimer:
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

        val responseCode = conn.responseCode
        val response = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
        }
        conn.disconnect()

        if (responseCode !in 200..299) {
            return "Sorry, AI is temporarily unavailable.\n($responseCode)"
        }

        val json = JSONObject(response)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).optString("text", "No answer received.")
            }
        }
        "Sorry, I couldn't generate an answer."
    } catch (e: Exception) {
        "Connection error. Please check your internet.\n(${e.message})"
    }
}

class AppUpdater(private val activity:Activity) {
    companion object {
        const val CURRENT_VERSION="1.2"
        const val API="https://api.github.com/repos/kawai-uhq/plaquote/releases/latest"
    }
    @Volatile var cancelled=false

    fun check(silent:Boolean) {
        thread {
            try {
                val c=URL(API).openConnection() as HttpURLConnection
                c.requestMethod="GET"; c.connectTimeout=10000; c.readTimeout=10000
                c.setRequestProperty("Accept","application/vnd.github+json")
                c.setRequestProperty("User-Agent","Plaquote-App")
                val json=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
                c.disconnect()
                val remote=json.optString("tag_name").removePrefix("v")
                val assets=json.optJSONArray("assets")
                var apk:String?=null
                if(assets!=null) for(i in 0 until assets.length()) {
                    val a=assets.getJSONObject(i)
                    if(a.optString("name").equals("Plaquote.apk",true)) apk=a.optString("browser_download_url")
                }
                if(!cancelled && apk!=null && newer(remote,CURRENT_VERSION)) {
                    activity.runOnUiThread { show(remote,apk!!) }
                }
            } catch(_:Exception) {}
        }
    }

    private fun newer(a:String,b:String):Boolean {
        val x=a.split(".").map{it.toIntOrNull()?:0}
        val y=b.split(".").map{it.toIntOrNull()?:0}
        for(i in 0 until maxOf(x.size,y.size)) {
            val aa=x.getOrElse(i){0}; val bb=y.getOrElse(i){0}
            if(aa!=bb) return aa>bb
        }
        return false
    }

    private fun show(v:String,url:String) {
        AlertDialog.Builder(activity)
            .setTitle("Plaquote update available")
            .setMessage("Version $v is available. Download and install it now?")
            .setNegativeButton("Later",null)
            .setPositiveButton("Update"){_,_->download(url,v)}
            .show()
    }

    private fun download(url:String,v:String) {
        thread {
            try {
                val dir=File(activity.cacheDir,"updates"); dir.mkdirs()
                val file=File(dir,"Plaquote-$v.apk")
                URL(url).openStream().use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
                activity.runOnUiThread { install(file) }
            } catch(e:Exception) {
                activity.runOnUiThread {
                    android.widget.Toast.makeText(activity,"Update download failed.",android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun install(file:File) {
        if(Build.VERSION.SDK_INT>=26 && !activity.packageManager.canRequestPackageInstalls()) {
            val intent=Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"))
            activity.startActivity(intent)
            android.widget.Toast.makeText(activity,"Allow Plaquote to install updates, then open the app again.",android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val uri=FileProvider.getUriForFile(activity,"${activity.packageName}.fileprovider",file)
        val intent=Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri,"application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}
