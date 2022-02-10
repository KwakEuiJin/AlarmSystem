package com.example.part3_chapter3

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import com.google.android.material.datepicker.DateValidatorPointBackward.before
import java.util.*

class MainActivity : AppCompatActivity() {

    private val onOffButton by lazy {
        findViewById<Button>(R.id.onOffButton)
    }
    private val changeAlarmTimeButton by lazy {
        findViewById<Button>(R.id.changeAlarmTimeButton)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //todo step0: 뷰 초기화
        initViews()
        //todo step1: 데이터 가져오기
        val model =fetchDataFromSharedPreference()
        //todo step2: 뷰에 데이터 연결
        renderView(model)


    }




    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initViews() {
        onOffButton.setOnClickListener {
            //데이터 확인
            val model = it.tag as? AlarnmDisplayModel ?: return@setOnClickListener
            //데어터 저장
            val newModel=saveAlarmModel(model.hour, model.minute, model.onOff.not())
            renderView(newModel)
            //온오프에 따른 작업설정
            if (newModel.onOff){
                //켜진경우 -> 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY,newModel.hour)
                    set(Calendar.MINUTE,newModel.minute)

                    if (before(Calendar.getInstance())){
                        add(Calendar.DATE,1)
                    }
                }
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this,AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this,
                    ALARM_REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT)
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )


            } else{
                //꺼진경우 -> 알람을 제거
                cancelAlarm()
            }


        }
        changeAlarmTimeButton.setOnClickListener {
            //현재시간 가져오기
            val calendar=Calendar.getInstance()
            //TimepickerDialog를 띄워서 시간설정
            TimePickerDialog(this,{picker, hour, minute ->

                //데이터 저장
                val model = saveAlarmModel(hour,minute,false)
                //뷰를 업데이트
                renderView(model)
                //기존의 알람 삭제(이부분은 db를 충원하여 삭제되지 않도록 할 수도 있음)
                cancelAlarm()

            },calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),false)
                .show()


        }
    }

    private fun saveAlarmModel(hour:Int, minute:Int, onOff:Boolean): AlarnmDisplayModel{
        val model=AlarnmDisplayModel(
            hour = hour,
            minute = minute,
            onOff)
        val sharedPreferences =getSharedPreferences("time",Context.MODE_PRIVATE)
        with(sharedPreferences.edit()){
            putString(ALARM_KEY,model.makeDataForDB())
            putBoolean(ONOFF_KEY,model.onOff)
            commit()
        }
        return model
    }
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun fetchDataFromSharedPreference(): AlarnmDisplayModel {
        val sharedPreferences =getSharedPreferences("time",Context.MODE_PRIVATE)
        val timeDB = sharedPreferences.getString(ALARM_KEY,"9:30") ?: "9:30"
        val onOffDB = sharedPreferences.getBoolean(ONOFF_KEY,false)
        val alarmData = timeDB.split(":")
        //Log.d("예약시간",alarmData[0].toString())
        val alarnmModel =AlarnmDisplayModel(
            hour=alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOffDB

        )
        //보정 예외처리
        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE,Intent(this,AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent==null && alarnmModel.onOff){
            //알람은 꺼져있는데 데이터는 켜저있는 경우
            alarnmModel.onOff=false
        }else if (pendingIntent !=null && alarnmModel.onOff.not()){
            //일림은 켜져있는데 데이터가 꺼져있는 경우
            pendingIntent.cancel() //알람취소
        }

        return alarnmModel
    }

    private fun renderView(model: AlarnmDisplayModel) {
        findViewById<TextView>(R.id.ampmTextView).apply {
            text= model.amPmText
        }
        findViewById<TextView>(R.id.timeTextView).apply {
            text=model.timeText
        }
        findViewById<Button>(R.id.onOffButton).apply {
            text=model.onOffText
            tag = model
        }

    }
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun cancelAlarm(){
        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE,Intent(this,AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.cancel()
    }


    companion object{
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE =1000
    }

}