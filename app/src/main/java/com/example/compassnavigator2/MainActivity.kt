package com.example.compassnavigator2

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var compassLayout: LinearLayout
    private lateinit var compassScrollView: HorizontalScrollView
    private lateinit var headingText: TextView

    private var accelerometerValues = FloatArray(3)
    private var magnetometerValues = FloatArray(3)

    private val directions = arrayOf(
        "N", "NE", "E", "SE", "S", "SW", "W", "NW"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        headingText = findViewById(R.id.headingText)
        compassLayout = findViewById(R.id.compassLayout)
        compassScrollView = findViewById(R.id.compassScrollView)

        // Repeat direction labels enough to fill scroll space
        for (i in 0..3) {  // Repeat to cover 360°
            for (dir in directions) {
                val label = TextView(this).apply {
                    text = dir
                    setTextColor(Color.WHITE)
                    setPadding(40, 0, 40, 0)
                    textSize = 24f
                }
                compassLayout.addView(label)
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
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
            // Scroll based on azimuth
            compassScrollView.post {
                val totalWidth = compassLayout.width
                val scrollTo = ((azimuth / 360.0) * totalWidth).toInt()
                val centerOffset = compassScrollView.width / 2
                compassScrollView.scrollTo(scrollTo - centerOffset, 0)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
