package com.dimanakhratiants.picture

import android.content.Context
import android.graphics.*
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.view.*
import java.util.*


/**
 * Created by dima on 27.10.17.
 */


class DrawingView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    var tool: Tool = Tool.PATH

    private val TOUCH_TOLERANCE_MIN = 1f
    private val TOUCH_TOLERANCE_MAX = 100f

    private val MAX_SCALE = 5f
    private val MIN_SCALE = 1f

    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val path = Path()
    private val paint = Paint()
    private val bitmapPaint: Paint
    private var background: Bitmap? = null

    private val scaleGestureDetector: ScaleGestureDetector

    var color = Color.BLACK
    var strokeWidth = 5f

    private var scaleFactor = 1f
    private var originX = 0f
    private var originY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var startX = 0f
    private var startY = 0f


    private var shapes = LinkedList<Shape>()

    init {
        scaleGestureDetector = ScaleGestureDetector(this.context, ScaleListener())

        bitmapPaint = Paint(Paint.DITHER_FLAG)

        paint.isAntiAlias = true
        paint.isDither = true

        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.setStrokeWidth(strokeWidth)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        originX = 0f
        originY = 0f
        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        canvas = Canvas(this.bitmap)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.v("dasd", shapes.size.toString())
        canvas.save()
        canvas.translate(0f, 0f)
        canvas.translate(originX, originY)
        canvas.scale(scaleFactor, scaleFactor)
        background?.let {
            canvas.drawBitmap(it, null, aspectFitRect(it), bitmapPaint)
        }
        for (shape in shapes) {

            paint.color = shape.color
            paint.strokeWidth = shape.width
            canvas.drawPath(shape.path, paint)
        }
        paint.color = color
        paint.strokeWidth = strokeWidth
        path?.let {
            canvas.drawPath(it, paint)
        }
        canvas.restore()
    }

    private fun touchStart(x: Float, y: Float) {
        path.reset()
        currentX = toGlobalCoords(x, Axis.X)
        currentY = toGlobalCoords(y, Axis.Y)
        startX = toGlobalCoords(x, Axis.X)
        startY = toGlobalCoords(y, Axis.Y)
        path.moveTo(startX, startY)
    }

    private fun touchMove(x: Float, y: Float) {
        val left = minOf(startX, toGlobalCoords(x, Axis.X))
        val right = maxOf(startX, toGlobalCoords(x, Axis.X))
        val top = minOf(startY, toGlobalCoords(y, Axis.Y))
        val bottom = maxOf(startY, toGlobalCoords(y, Axis.Y))

        when (tool) {
            Tool.PATH -> {
                val dx = Math.abs(toGlobalCoords(x, Axis.X) - currentX)
                val dy = Math.abs(toGlobalCoords(y, Axis.Y) - currentY)
                if ((dx >= TOUCH_TOLERANCE_MIN || dy >= TOUCH_TOLERANCE_MIN) &&
                        (dx <= TOUCH_TOLERANCE_MAX && dy <= TOUCH_TOLERANCE_MAX)) {
                    path.quadTo(currentX,
                            currentY,
                            (toGlobalCoords(x, Axis.X) + currentX) / 2,
                            (toGlobalCoords(y, Axis.Y) + currentY) / 2)
                }
            }
            Tool.RECT -> {
                path.reset()
                path.addRect(left, top, right, bottom, Path.Direction.CCW)
            }
            Tool.CIRCLE -> {
                path.reset()
                path.addOval(left, top, right, bottom, Path.Direction.CCW)
            }
            Tool.MOVE -> {
                val dx = currentX - toGlobalCoords(x, Axis.X)
                val dy = currentY - toGlobalCoords(y, Axis.Y)
                originX -= dx * scaleFactor
                originY -= dy * scaleFactor

                if (originX < width - width * scaleFactor) {
                    originX = width - width * scaleFactor
                }
                if (originY < height - height * scaleFactor) {
                    originY = height - height * scaleFactor
                }
                if (originX > 0) {
                    originX = 0f
                }
                if (originY > 0) {
                    originY = 0f
                }
            }
        }
        currentX = toGlobalCoords(x, Axis.X)
        currentY = toGlobalCoords(y, Axis.Y)
    }

    private fun touchUp() {
        shapes.add(Shape(color = this.color, width = strokeWidth, path = Path(path)))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1 && tool == Tool.MOVE) {
            scaleGestureDetector.onTouchEvent(event)
        } else {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStart(event.x, event.y)
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    touchMove(event.x, event.y)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    touchUp()
                    invalidate()
                }

            }
        }
        return true
    }

    private fun toGlobalCoords(coord: Float, axis: Axis): Float {
        return when (axis) {
            Axis.X -> coord / (scaleFactor) - (originX / scaleFactor)
            Axis.Y -> coord / (scaleFactor) - (originY / scaleFactor)
        }
    }

    private fun aspectFitRect(bitmap: Bitmap): RectF {
        val rect = RectF()
        val xMultiplier = bitmap.width / width
        val yMultiplier = bitmap.height / height
        if (xMultiplier > 1 || yMultiplier > 1) {
            val multiplier = when (xMultiplier > yMultiplier) {
                true -> xMultiplier
                false -> yMultiplier
            }
            val height = bitmap.height / multiplier
            val width = bitmap.width / multiplier

            rect.left = ((width - width) / 2).toFloat()
            rect.top = ((height - height) / 2).toFloat()
            rect.right = rect.left + width
            rect.bottom = rect.top + height

        } else {
            rect.left = ((width - bitmap.width) / 2).toFloat()
            rect.top = ((height - bitmap.height) / 2).toFloat()
            rect.right = rect.left + bitmap.width
            rect.bottom = rect.top + bitmap.height
        }
        return rect
    }

    inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector?.let {
                val scale = scaleFactor * it.scaleFactor
                scaleFactor = maxOf(MIN_SCALE, minOf(scale, MAX_SCALE))
            }
            invalidate()
            return true
        }
    }

    fun drawImage(bitmap: Bitmap) {
        shapes.clear()
        this.background = bitmap
        invalidate()
    }

    fun undo() {
        if (!shapes.isEmpty()) {
            shapes.removeLast()
            path.reset()
        }
        invalidate()
    }

    fun getImage(): Bitmap? {
        var image: Bitmap
        image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        image.eraseColor(Color.WHITE)
        val canvas = Canvas(image)
        background?.let {
            canvas.drawBitmap(it, null, aspectFitRect(it), bitmapPaint)
        }
        for (shape in shapes) {
            paint.color = shape.color
            paint.strokeWidth = shape.width
            canvas.drawPath(shape.path, paint)
        }
        return image
    }

}