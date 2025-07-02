package com.example.compassnavigator2

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.TypedValue
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var compassLayout: LinearLayout
    private lateinit var compassScrollView: HorizontalScrollView
    private lateinit var headingText: TextView

    private var accelerometerValues = FloatArray(3)
    private var magnetometerValues = FloatArray(3)

    private val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        headingText = findViewById(R.id.headingText)
        compassLayout = findViewById(R.id.compassLayout)
        compassScrollView = findViewById(R.id.compassScrollView)

        // Convert a large dp value to pixels so only one label is visible at a time
        val labelWidthDp = 300f // Wide enough so only one is centered
        val labelWidthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, labelWidthDp, resources.displayMetrics
        ).toInt()

        directions.forEach { dir ->
            val label = TextView(this).apply {
                text = dir
                setTextColor(Color.WHITE)
                textSize = 24f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                layoutParams = LinearLayout.LayoutParams(labelWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            compassLayout.addView(label)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> accelerometerValues = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> magnetometerValues = event.values.clone()
        }

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magnetometerValues)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuth = (Math.toDegrees(orientationAngles[0].toDouble()) + 360) % 360
            headingText.text = "Heading: %.1f°".format(azimuth)

            compassScrollView.post {
                val labelWidth = compassLayout.getChildAt(0)?.width ?: return@post
                val totalLabels = directions.size

                // Each label represents 360 / totalLabels degrees
                val degreesPerLabel = 360f / totalLabels

                // Determine which label corresponds to azimuth
                val indexFloat = azimuth / degreesPerLabel
                val exactPosition = indexFloat * labelWidth
                val centerOffset = compassScrollView.width / 2 - labelWidth / 2

                val currentX = compassScrollView.scrollX
                val targetX = exactPosition.toInt() - centerOffset

                ValueAnimator.ofInt(currentX, targetX).apply {
                    duration = 250L
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { animator ->
                        compassScrollView.scrollTo(animator.animatedValue as Int, 0)
                    }
                    start()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
