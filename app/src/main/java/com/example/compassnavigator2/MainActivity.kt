package com.example.compassnavigator2

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var compassContainer: FrameLayout
    private lateinit var headingText: TextView

    private val labelViews = mutableMapOf<String, TextView>()
    private val labelHeadings = mapOf(
        "N" to 0f,
        "NE" to 45f,
        "E" to 90f,
        "SE" to 135f,
        "S" to 180f,
        "SW" to 225f,
        "W" to 270f,
        "NW" to 315f
        // Add more here later like: "E" to 90f, "S" to 180f, etc.
    )

    private var accelerometerValues = FloatArray(3)
    private var magnetometerValues = FloatArray(3)
    private val labelVisibility = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        headingText = findViewById(R.id.headingText)
        compassContainer = findViewById(R.id.compassContainer)

        // Wait until layout is measured
        compassContainer.post {
            val screenHeight = compassContainer.height

            labelHeadings.forEach { (label, _) ->
                val labelView = TextView(this).apply {
                    text = label
                    setTextColor(Color.WHITE)
                    textSize = 48f
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        // Set top margin to half of screen height (approx vertical center)
                        topMargin = screenHeight / 2
                    }
                }
                compassContainer.addView(labelView)
                labelViews[label] = labelView
                labelVisibility[label] = false
            }
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

            val screenWidth = compassContainer.width

            labelHeadings.forEach { (label, targetHeading) ->
                val labelView = labelViews[label] ?: return@forEach

                val labelWidth = labelView.width
                val centerX = (screenWidth - labelWidth) / 2f

                var screenWidthMultiplier = 4
                val maxOffset = screenWidth / 2f * screenWidthMultiplier

                val diff = ((targetHeading - azimuth + 540) % 360) - 180
                val offsetPx = (diff / 180f) * maxOffset

                val isVisible = abs(diff) <= 90f
                val wasVisible = labelVisibility[label] ?: false

                if (isVisible) {
                    labelView.visibility = View.VISIBLE

                    if (wasVisible) {
                        // Animate smoothly
                        labelView.animate()
                            .translationX((centerX + offsetPx).toFloat())
                            .setDuration(75L)
                            .start()
                    } else {
                        // Jump to correct spot instantly
                        labelView.translationX = (centerX + offsetPx).toFloat()
                    }
                } else {
                    labelView.visibility = View.INVISIBLE
                }

                // Update visibility state
                labelVisibility[label] = isVisible
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
