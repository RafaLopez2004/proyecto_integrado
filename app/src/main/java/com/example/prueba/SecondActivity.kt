package com.example.prueba

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.prueba.firebase.checkIn
import com.example.prueba.firebase.checkLocation
import com.example.prueba.firebase.checkOut
import com.example.prueba.firebase.logOut
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SecondActivity  : AppCompatActivity(){

    private val TAG = MainActivity::getLocalClassName.toString()
    private val LOC_CODE = 1000
    private val LOC_CODE_OUT = 1001
    private var safe : Boolean = true
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var context : Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*If the biometric services are available we continue with the program,
        otherwise we show an alert to the user*/
        /*if (!checkBiometricAvailable()) {
            //Registers not verified check in
            AlertDialog.Builder(this)
                .setMessage("Your dispositive does not have the required hardware to do a safe check in")
                .setPositiveButton("OK", null )
                .show()
            safe = false
        }*/
        setContentView(R.layout.login)
        val inButton: Button = findViewById(R.id.inbtn)
        val outButton: Button = findViewById(R.id.outbtn)
        val logOut: TextView = findViewById(R.id.logout)
        var operating = false
        context = this

        // Check in button logic
        inButton.setOnClickListener {
            if (!operating){
                operating = true
                CoroutineScope(Dispatchers.IO).launch {
                    if (checkPermission())
                        checkInLocal(context)
                    else
                        ActivityCompat.requestPermissions(context as SecondActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOC_CODE)
                    operating = false
                } }
            else
                Toast.makeText(context, "Espera a que termine la operación anterior", Toast.LENGTH_LONG).show()
        }
        // Check out button logic
        outButton.setOnClickListener {
            if (!operating){
            CoroutineScope(Dispatchers.IO).launch {
                operating = true
                if (checkPermission())
                    checkOutLocal(context)
                else
                    ActivityCompat.requestPermissions(context as SecondActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOC_CODE_OUT)
                operating = false
            } }
            else
                Toast.makeText(context, "Espera a que termine la operación anterior", Toast.LENGTH_LONG).show()
        }
        // LogOut button logic
        logOut.setOnClickListener {
            logOut()
            this.startActivity(Intent(this, MainActivity::class.java))
        }
    }
    /**
     * This method checks if biometric information is availble
     *//*
    private fun checkBiometricAvailable() : Boolean{
        return when(BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)){
            BiometricManager.BIOMETRIC_SUCCESS ->{
                Log.d(TAG,"Can use")
                true
            }
            else ->{
                Log.d(TAG, "Cant use")
                false
            }
        }
    }
    private fun biometric(): BiometricPrompt.PromptInfo {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext,
                        "Authentication succeeded!", Toast.LENGTH_SHORT)
                        .show()
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Aaaa")
            .build()
        biometricPrompt.authenticate(promptInfo)
        return promptInfo
    }*/

    // FUNCTIONS FOR THE CHECK IN AND OUT
    private suspend fun checkInLocal(context : Context){
        var toastText = ""
        val location = getLocation()
        // We check if we are in range to be able to to do the check in
        if (location != null && checkLocation(location)) {
            // We try to check in
            when (checkIn(safe)) {
                1 ->
                    toastText = "Check in successful"
                0 ->
                    toastText = "You already checked in"
                -1 ->
                    toastText = "An error occurred while checking in"
            } }
        else
            toastText = "You arent in range to check in"
        // Print the result of the operation
        runOnUiThread { Toast.makeText(context, toastText, Toast.LENGTH_LONG).show() }
    }

    private suspend fun checkOutLocal(context: Context){
        var toastText = ""
        val location = getLocation()
        // We check if we are in range to be able to to do the check out
        if (location != null && checkLocation(location)) {
            when (checkOut()) {
                1 ->
                    toastText = "Check out successful"
                0 ->
                    toastText = "You haven't checked in today"
                -1 ->
                    toastText = "An error occurred while checking out"
            } }
        else
            toastText = "You arent in range to check out"
        runOnUiThread { Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        }
    }
    // FUNCTIONS RELATED TO THE POSSITION

    /**
     *
     */
    @SuppressLint("MissingPermission")
    private suspend fun getLocation(): Location? {
        fusedLocationClient= LocationServices.getFusedLocationProviderClient(this)
        return fusedLocationClient.lastLocation.await()
    }
    /**
     * Check if we have Location permission
     */
    private fun checkPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)}

    override fun onDestroy() {
        super.onDestroy()
        if (intent.extras?.getBoolean("remember") == false)
            logOut()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val toastText = "We require your location to check if you are in your workplace"
        when(requestCode){
            LOC_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    CoroutineScope(Dispatchers.IO).launch {checkInLocal(context)}
                else
                    Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            }
            LOC_CODE_OUT -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    CoroutineScope(Dispatchers.IO).launch {checkInLocal(context)}
                else
                    Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            }
        }
    }
}