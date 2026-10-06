package com.plaquote.app

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.*
import android.content.Context
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(PlaquoteView(this))
    }
}

class PlaquoteView(context: Context) : View(context) {
    private val blue = Color.rgb(22, 119, 240)
    private val darkBlue = Color.rgb(7, 82, 199)
    private var darkMode = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val toothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(226, 238, 252)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        c.drawColor(if (darkMode) Color.rgb(11,18,32) else Color.WHITE)

        drawPattern(c, w, h)

        val header = Path()
        header.moveTo(0f, 0f)
        header.lineTo(w, 0f)
        header.lineTo(w, h*.15f)
        header.cubicTo(w*.78f,h*.20f,w*.62f,h*.06f,w*.44f,h*.13f)
        header.cubicTo(w*.25f,h*.21f,w*.12f,h*.18f,0f,h*.24f)
        header.close()
        paint.shader = LinearGradient(0f,0f,w,h*.24f,blue,darkBlue,Shader.TileMode.CLAMP)
        c.drawPath(header, paint)
        paint.shader = null

        val wave = Path()
        wave.moveTo(0f,h*.17f)
        wave.cubicTo(w*.20f,h*.24f,w*.33f,h*.17f,w*.48f,h*.14f)
        wave.cubicTo(w*.66f,h*.10f,w*.77f,h*.20f,w,h*.13f)
        wave.lineTo(w,h*.19f)
        wave.cubicTo(w*.77f,h*.26f,w*.61f,h*.16f,w*.47f,h*.20f)
        wave.cubicTo(w*.28f,h*.27f,w*.12f,h*.24f,0f,h*.22f)
        wave.close()
        paint.color = Color.rgb(49,181,250)
        paint.alpha = 210
        c.drawPath(wave,paint)
        paint.alpha = 255

        drawToggle(c,24f,28f,174f,54f)

        paint.color = Color.argb(65,255,255,255)
        c.drawCircle(w-54f,55f,28f,paint)
        drawSearch(c,w-54f,55f)

        drawAi(c,24f,h-78f)

        paint.setShadowLayer(18f,0f,7f,Color.argb(80,0,85,200))
        setLayerType(LAYER_TYPE_SOFTWARE,paint)
        paint.color=blue
        c.drawCircle(w-58f,h-60f,31f,paint)
        paint.clearShadowLayer()
        paint.color=Color.WHITE
        paint.strokeWidth=5f
        paint.strokeCap=Paint.Cap.ROUND
        c.drawLine(w-72f,h-60f,w-44f,h-60f,paint)
        c.drawLine(w-58f,h-74f,w-58f,h-46f,paint)
        paint.strokeCap=Paint.Cap.BUTT
    }

    private fun drawPattern(c:Canvas,w:Float,h:Float) {
        val xs=floatArrayOf(.07f,.24f,.43f,.64f,.84f,.16f,.35f,.55f,.75f,.94f,.05f,.28f,.49f,.70f,.88f)
        val ys=floatArrayOf(.31f,.39f,.29f,.43f,.34f,.57f,.51f,.61f,.54f,.65f,.78f,.70f,.82f,.73f,.88f)
        for(i in xs.indices) {
            drawTooth(c,xs[i]*w,ys[i]*h,26f+(i%3)*7f)
            paint.color=Color.rgb(235,243,253)
            c.drawCircle(xs[i]*w+35f,ys[i]*h+18f,4.5f,paint)
        }
    }

    private fun drawTooth(c:Canvas,cx:Float,cy:Float,s:Float) {
        val p=Path()
        p.moveTo(cx-s*.48f,cy-s*.28f)
        p.cubicTo(cx-s*.65f,cy-s*.70f,cx-s*.15f,cy-s*.78f,cx,cy-s*.50f)
        p.cubicTo(cx+s*.18f,cy-s*.78f,cx+s*.65f,cy-s*.65f,cx+s*.47f,cy-s*.25f)
        p.cubicTo(cx+s*.40f,cy+.05f*s,cx+s*.30f,cy+.10f*s,cx+s*.20f,cy+s*.48f)
        p.cubicTo(cx+s*.08f,cy+s*.72f,cx-s*.08f,cy+s*.72f,cx-s*.18f,cy+s*.43f)
        p.cubicTo(cx-s*.28f,cy+s*.12f,cx-s*.40f,cy+.04f*s,cx-s*.48f,cy-s*.28f)
        c.drawPath(p,toothPaint)
    }

    private fun drawToggle(c:Canvas,x:Float,y:Float,w:Float,h:Float) {
        paint.color=Color.argb(190,0,50,130)
        c.drawRoundRect(x,y,x+w,y+h,h/2,h/2,paint)
        val k=if(darkMode)x+w-h/2 else x+h/2
        paint.color=Color.WHITE
        c.drawCircle(k,y+h/2,h/2-5f,paint)
        paint.color=if(darkMode)Color.rgb(210,225,250) else blue
        paint.textSize=24f
        paint.textAlign=Paint.Align.CENTER
        c.drawText(if(darkMode)"☾" else "☀",k,y+h/2+8f,paint)
        paint.textAlign=Paint.Align.LEFT
    }

    private fun drawSearch(c:Canvas,cx:Float,cy:Float) {
        paint.color=Color.WHITE
        paint.style=Paint.Style.STROKE
        paint.strokeWidth=4f
        c.drawCircle(cx-3f,cy-3f,9f,paint)
        c.drawLine(cx+4f,cy+4f,cx+12f,cy+12f,paint)
        paint.style=Paint.Style.FILL
    }

    private fun drawAi(c:Canvas,x:Float,y:Float) {
        paint.color=blue
        c.drawRoundRect(x,y,x+152f,y+52f,26f,26f,paint)
        paint.color=Color.WHITE
        c.drawRoundRect(x+15f,y+15f,x+52f,y+40f,8f,8f,paint)
        paint.color=blue
        c.drawCircle(x+26f,y+27f,3f,paint)
        c.drawCircle(x+41f,y+27f,3f,paint)
        paint.strokeWidth=2f
        c.drawLine(x+27f,y+35f,x+40f,y+35f,paint)
        paint.color=Color.WHITE
        paint.textSize=18f
        paint.typeface=Typeface.create("sans",Typeface.BOLD)
        c.drawText("AI Help",x+64f,y+33f,paint)
        paint.typeface=Typeface.DEFAULT
    }

    override fun onTouchEvent(e:MotionEvent):Boolean {
        if(e.action!=MotionEvent.ACTION_UP)return true
        val x=e.x; val y=e.y; val w=width.toFloat(); val h=height.toFloat()

        if(x in 24f..198f && y in 28f..82f) {
            darkMode=!darkMode
            invalidate()
            Toast.makeText(context,if(darkMode)"Dark mode enabled" else "Light mode enabled",Toast.LENGTH_SHORT).show()
        } else if((x-(w-54f))*(x-(w-54f))+(y-55f)*(y-55f)<35f*35f) {
            Toast.makeText(context,"Search — coming soon",Toast.LENGTH_SHORT).show()
        } else if(x in 24f..176f && y>h-95f) {
            Toast.makeText(context,"AI Help — coming soon",Toast.LENGTH_SHORT).show()
        } else if((x-(w-58f))*(x-(w-58f))+(y-(h-60f))*(y-(h-60f))<45f*45f) {
            Toast.makeText(context,"New patient note — coming soon",Toast.LENGTH_SHORT).show()
        }
        return true
    }
}
