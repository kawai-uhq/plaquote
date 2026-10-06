package com.plaquote.app

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

    private fun drawTooth(c:Canvas,x:Float,y:Float,s:Float) {
        val q=Path()
        q.moveTo(x-s*.48f,y-s*.28f)
        q.cubicTo(x-s*.65f,y-s*.70f,x-s*.15f,y-s*.78f,x,y-s*.50f)
        q.cubicTo(x+s*.18f,y-s*.78f,x+s*.65f,y-s*.65f,x+s*.47f,y-s*.25f)
        q.cubicTo(x+s*.40f,y+s*.05f,x+s*.30f,y+s*.10f,x+s*.20f,y+s*.48f)
        q.cubicTo(x+s*.08f,y+s*.72f,x-s*.08f,y+s*.72f,x-s*.18f,y+s*.43f)
        q.cubicTo(x-s*.28f,y+s*.12f,x-s*.40f,y+s*.04f,x-s*.48f,y-s*.28f)
        c.drawPath(q,tooth)
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

    override fun onTouchEvent(e:MotionEvent):Boolean {
        if(e.action!=MotionEvent.ACTION_UP)return true
        val x=e.x; val y=e.y; val w=width.toFloat(); val h=height.toFloat()
        if(x in 24f..198f && y in 28f..82f) {
            dark=!dark; invalidate()
        } else if((x-(w-54f))*(x-(w-54f))+(y-55f)*(y-55f)<35f*35f) {
            android.widget.Toast.makeText(ctx,"Search — coming soon",android.widget.Toast.LENGTH_SHORT).show()
        } else if(x in 24f..176f && y>h-95f) {
            android.widget.Toast.makeText(ctx,"AI Help — coming soon",android.widget.Toast.LENGTH_SHORT).show()
        }
        return true
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
