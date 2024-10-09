package com.sensor.heatlh

import android.R
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.sensor.heatlh.databinding.ActivityHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.dump.InvalidFormatException
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.IOException
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    var respRateValue = ""
    var hearRateValue = ""
    var xAxisList = ArrayList<Float>()
    var yAxisList = ArrayList<Float>()
    var zAxisList = ArrayList<Float>()
    private var videoCapture: VideoCapture<Recorder>? = null
    private lateinit var previewView: PreviewView
    private var camera: Camera? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()

    }

    private fun initUI() {
        checkPermission()
        loadXAxisList()
        loadYAxisList()
        loadZAxisList()
        binding.btnSymptoms.setOnClickListener {
            moveToSymptomsActivity()
        }
        binding.btnRespRate.setOnClickListener {
            respRateValue = respiratoryRateCalculator(
                xAxisList,
                yAxisList,
                zAxisList
            ).toString()
            binding.tvRespRateValue.text = "Respiratory Rate : ".plus(respRateValue)
        }
        binding.btnHeartRate.setOnClickListener {
            //binding.rlProgress.visibility = View.VISIBLE
            GlobalScope.launch {
                heartRateCalculator()
            }

        }

    }


    private fun checkPermission(){
        if (ContextCompat.checkSelfPermission(
                this,
                "android.permission.READ_EXTERNAL_STORAGE"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.READ_EXTERNAL_STORAGE"),
                1
            )
        }
    }



    private fun moveToSymptomsActivity() {
        var intent = Intent(this, UploadSymptomsActivity::class.java)
        intent.putExtra("heartRate", hearRateValue)
        intent.putExtra("respRate", respRateValue)
        startActivity(intent)
    }

    fun respiratoryRateCalculator(
        accelValuesX: MutableList<Float>,
        accelValuesY: MutableList<Float>,
        accelValuesZ: MutableList<Float>,
    ): Int {
        var previousValue: Float
        var currentValue: Float
        previousValue = 10f
        var k = 0
        for (i in 11..<accelValuesY.size) {
            currentValue = kotlin.math.sqrt(
                accelValuesZ[i].toDouble().pow(2.0) + accelValuesX[i].toDouble()
                    .pow(2.0) + accelValuesY[i].toDouble().pow(2.0)
            ).toFloat()
            if (abs(x = previousValue - currentValue) > 0.15) {
                k++
            }
            previousValue = currentValue
        }
        val ret = (k.toDouble() / 45.00)
        return (ret * 30).toInt()
    }

     suspend fun heartRateCalculator() {
         return withContext(Dispatchers.Main) {
             val result: Int
             var projection = arrayOf(
                 MediaStore.Video.Media._ID,
                 MediaStore.Video.Media.DATA,
                 MediaStore.Video.Media.DISPLAY_NAME,
                 MediaStore.Video.Media.SIZE
             )
             val selection = MediaStore.Video.Media.DATA + " like?"
             val selectionArgs = arrayOf("%Health%")
             val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
             val cursor = contentResolver.query(
                 uri,
                 projection,
                 selection,
                 selectionArgs,
                 MediaStore.Video.Media.DATE_ADDED + " DESC"
             )
             val columnIndex =
                 cursor?.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
             cursor?.moveToFirst()
            val path = cursor?.getString(columnIndex ?: 0)
             cursor?.close()

             val retriever = MediaMetadataRetriever()
             val frameList = ArrayList<Bitmap>()
             try {
                 retriever.setDataSource(path)
                 val duration =
                     retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                 val frameDuration = min(duration!!.toInt(), 425)
                 var i = 10
                 while (i < frameDuration) {
                     val bitmap = retriever.getFrameAtIndex(i)
                     bitmap?.let { frameList.add(it) }
                     i += 15
                 }
             } catch (e: Exception) {
                 Log.d("MediaPath", "convertMediaUriToPath: ${e.stackTrace} ")
             } finally {
                 retriever.release()
                 var redBucket: Long
                 var pixelCount: Long = 0
                 val a = mutableListOf<Long>()
                 for (i in frameList) {
                     redBucket = 0
                     for (y in 350 until 450) {
                         for (x in 350 until 450) {
                             val c: Int = i.getPixel(x, y)
                             pixelCount++
                             redBucket += Color.red(c) + Color.blue(c) +
                                     Color.green(c)
                         }
                     }
                     a.add(redBucket)
                 }
                 val b = mutableListOf<Long>()
                 for (i in 0 until a.lastIndex - 5) {
                     val temp =
                         (a.elementAt(i) + a.elementAt(i + 1) + a.elementAt(i + 2)
                                 + a.elementAt(
                             i + 3
                         ) + a.elementAt(
                             i + 4
                         )) / 4
                     b.add(temp)
                 }
                 var x = b.elementAt(0)
                 var count = 0
                 for (i in 1 until b.lastIndex) {
                     val p = b.elementAt(i)
                     if ((p - x) > 3500) {
                         count += 1
                     }
                     x = b.elementAt(i)
                 }
                 val rate = ((count.toFloat()) * 60).toInt()
                 result = (rate / 4)
             }
             binding.rlProgress.visibility = View.GONE
             hearRateValue = result.toString()
             binding.tvHeartRateValue.text = "Heart Rate : ".plus(hearRateValue)
//             result
         }
     }

    private fun loadXAxisList() {
        try {
//            val inputStream = contentResolver.openInputStream(uri)
            val inputStream = getAssets().open("CSVBreatheX.xlsx")
            val workbook = WorkbookFactory.create(inputStream)
            val sheet: Sheet = workbook.getSheetAt(0) // Assuming you want the first sheet

            // Iterate through rows and cells
            val rowIterator: Iterator<Row> = sheet.iterator()
            while (rowIterator.hasNext()) {
                val row: Row = rowIterator.next()
                val cellIterator: Iterator<Cell> = row.cellIterator()
                while (cellIterator.hasNext()) {
                    val cell = cellIterator.next()
                    xAxisList.add(cell.numericCellValue.toFloat())
                }
            }
            workbook.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvalidFormatException) {
            e.printStackTrace()
        }
    }

    private fun loadYAxisList() {
        try {
//            val inputStream = contentResolver.openInputStream(uri)
            val inputStream = getAssets().open("CSVBreatheY.xlsx")
            val workbook = WorkbookFactory.create(inputStream)
            val sheet: Sheet = workbook.getSheetAt(0) // Assuming you want the first sheet

            // Iterate through rows and cells
            val rowIterator: Iterator<Row> = sheet.iterator()
            while (rowIterator.hasNext()) {
                val row: Row = rowIterator.next()
                val cellIterator: Iterator<Cell> = row.cellIterator()
                while (cellIterator.hasNext()) {
                    val cell = cellIterator.next()
                    yAxisList.add(cell.numericCellValue.toFloat())
                }
            }
            workbook.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvalidFormatException) {
            e.printStackTrace()
        }
    }

    private fun loadZAxisList() {
        try {
//            val inputStream = contentResolver.openInputStream(uri)
            val inputStream = getAssets().open("CSVBreatheZ.xlsx")
            val workbook = WorkbookFactory.create(inputStream)
            val sheet: Sheet = workbook.getSheetAt(0) // Assuming you want the first sheet

            // Iterate through rows and cells
            val rowIterator: Iterator<Row> = sheet.iterator()
            while (rowIterator.hasNext()) {
                val row: Row = rowIterator.next()
                val cellIterator: Iterator<Cell> = row.cellIterator()
                while (cellIterator.hasNext()) {
                    val cell = cellIterator.next()
                    zAxisList.add(cell.numericCellValue.toFloat())
                }
            }
            workbook.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvalidFormatException) {
            e.printStackTrace()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                preview.setSurfaceProvider(previewView.surfaceProvider)

                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )

            } catch (exc: Exception) {
                Toast.makeText(this, "Error starting camera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

}

