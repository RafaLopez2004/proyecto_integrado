package com.example.prueba

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.example.prueba.firebase.checkIn
import com.example.prueba.firebase.checkOut
import com.example.prueba.firebase.logOut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecondActivity  : AppCompatActivity(){

    private var TAG = MainActivity::getLocalClassName.toString()
    private var safe : Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*If the biometric services are available we continue with the program,
        otherwise we show an alert to the user*/
        if (!checkBiometricAvailable()) {
            //Registers not verified check in
            val builder = AlertDialog.Builder(this)
                .setMessage("Your dispositive does not have the required hardware to do a safe check in")
                .setPositiveButton("OK", null )
                .show()
            builder.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{safe = false}
        }
        setContentView(R.layout.login)
        val inButton: Button = findViewById(R.id.inbtn)
        val outButton: Button = findViewById(R.id.outbtn)

        val context = this
        inButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val result = checkIn(safe)
                runOnUiThread {
                    when (result){
                        1->
                            Toast.makeText(context, "Check in successful",
                                Toast.LENGTH_LONG).show()
                        0->
                            Toast.makeText(context, "You already checked in today",
                                Toast.LENGTH_LONG).show()
                        -1 ->
                            Toast.makeText(context, "An error occurred while checking in",
                                Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        outButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val result = checkOut()
                runOnUiThread {
                    when (result){
                        1->
                            Toast.makeText(context, "Check out successful",
                                Toast.LENGTH_LONG).show()
                        0->
                            Toast.makeText(context, "You haven't checked in today",
                                Toast.LENGTH_LONG).show()
                        -1 ->
                            Toast.makeText(context, "An error occurred while checking out",
                                Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    /**
     * This method checks if biometric information is availble
     */
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
    /*
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
    override fun onDestroy() {
        super.onDestroy()
        if (intent.extras?.getBoolean("remember") == false)
            logOut()
    }
}