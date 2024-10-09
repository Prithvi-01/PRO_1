package com.sensor.heatlh

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sensor.heatlh.databinding.ActivityUploadSymptomsBinding

class UploadSymptomsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadSymptomsBinding
    val layoutResourceId = R.layout.activity_upload_symptoms
    var dbSymptomsList = ArrayList<PatientData>()
    var dbRateList = PatientData()
    var symptomsList =  arrayOf<String>("Select Symptoms","Nausea","Headache","diarrhea","Soar Throat","Fever","Muscle Ache","Loss of smell or taste","Cough","Shortness of Breath","Feeling tired")
    var selectedPos = 0
    var patientId = System.currentTimeMillis()
    var heartRate = ""
    var respRate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadSymptomsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initUI()
        }

    private fun initUI() {
        heartRate = intent.getStringExtra("heartRate").toString()
        respRate = intent.getStringExtra("respRate").toString()
        loadSpinner()
        loadDefaultRating()
        binding.btnUpload.setOnClickListener { uploadAllSymptomsData() }
        binding.btnRatingSubmit.setOnClickListener {
            if(selectedPos == 0){
                Toast.makeText(
                    applicationContext, "Kindly select symptoms",
                    Toast.LENGTH_SHORT
                ).show()
            } else if(binding.ratingbar.rating == 0.0F){
                Toast.makeText(
                    applicationContext, "Kindly select rating",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                submitRating()
            }
        }
    }

    private fun loadDefaultRating() {

        symptomsList.forEach {
            if(!it.equals("Select Symptoms")){
                var symptoms = PatientData()
                symptoms.respRate = respRate
                symptoms.heartRate = heartRate
                symptoms.patientId = patientId
                symptoms.symptoms = it
                symptoms.symptomsStarRate = "0"
                dbSymptomsList.add(symptoms)
            }
        }
    }

    private fun submitRating() {
        if(dbSymptomsList.size > 0) {
            dbSymptomsList.forEach{
                if(it.symptoms.equals(symptomsList[selectedPos], true)) {
                    it.symptomsStarRate = binding.ratingbar.rating.toString()
                    Toast.makeText(
                        applicationContext,
                        symptomsList[selectedPos].plus(" rating submitted succesfully"),
                        Toast.LENGTH_SHORT
                    ).show()
                    resetUI()
                }
            }
        }
    }

    private fun resetUI() {
        binding.ratingbar.rating = 0.0F
        binding.spinner.setSelection(0)
    }

    private fun uploadAllSymptomsData() {
       var dbInstance = HealthDatabase.getDBInstance(applicationContext)
        dbInstance.patientDao().insert(dbSymptomsList)
//        dbInstance.symptomsDao().insert(dbSymptomsList)
        Toast.makeText(
            applicationContext, "Data uploaded to Database successfully",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun loadSpinner(){
        val adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, symptomsList)
        binding.spinner.adapter = adapter
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                if(pos != 0) {
                    selectedPos = pos
                }
//             Toast.makeText(applicationContext, symptomsList[pos], Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }

    }
}