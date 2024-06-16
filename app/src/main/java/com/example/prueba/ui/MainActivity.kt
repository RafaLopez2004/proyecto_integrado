package com.example.prueba.ui


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.prueba.R

import com.example.prueba.firebase.forgotPass
import com.example.prueba.firebase.getCurrentUser
import com.example.prueba.firebase.logIn
import com.example.prueba.firebase.getCurrentUserDocument

import com.google.android.material.button.MaterialButton

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var user : EditText
    private lateinit var password : EditText
    private lateinit var forgot : TextView
    private lateinit var wrong : TextView
    private lateinit var login : MaterialButton
    private lateinit var remember : CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //If there is an alredy logged in
        if (getCurrentUser() != null) {
            moveNext(true)
            CoroutineScope(Dispatchers.IO).launch { getCurrentUserDocument() }
        }
        setContentView(R.layout.activity_main)
        //Instancing all needed variables
        user = findViewById(R.id.username)
        password = findViewById(R.id.password)
        forgot = findViewById(R.id.forgotpass)
        wrong = findViewById(R.id.wrongCredetials)
        login = findViewById(R.id.loginbtn)
        remember = findViewById(R.id.remember)
        //When presses, it will try to login the user,
        login.setOnClickListener {
            // We check if the user has inputted the credentials
            if(user.text.isEmpty() || password.text.isEmpty()){
                wrong.setText(R.string.incomplete_credentials)
                wrong.isVisible = true
            } else CoroutineScope(Dispatchers.IO).launch {
                if(logIn(user.text.toString(), password.text.toString()) == true) {
                    //If successful, we'll move to the next activity
                    moveNext(remember.isChecked)
                } else runOnUiThread {
                    //Otherwise we'll show an error message to the user
                    password.text.clear()
                    wrong.setText(R.string.invalid_credentials)
                    wrong.isVisible = true
                }
            }
        }
        forgot.setOnClickListener {
            val context = this
            //dialogue asking for the email we want to reset pass
            val builder = AlertDialog.Builder(context)
            builder.setTitle(getString(R.string.introduce_mail))
            val inflatedView = LayoutInflater.from(context)
                .inflate(R.layout.forgot_dialog, findViewById(R.id.main), false)
            val input = inflatedView.findViewById<EditText>(R.id.input)
            builder.setView(inflatedView)
            // Button to send he email o the direction given by the user
            builder.setPositiveButton("Ok") { _, _ ->
                if (input.text.toString().isBlank())
                    Toast.makeText(context, getString(R.string.introduce_mail_field), Toast.LENGTH_SHORT).show()
                else {
                    var response = ""
                    CoroutineScope(Dispatchers.IO).launch {
                        //Reset method
                        when (forgotPass(input.text.toString())) {
                            -1 -> response =  getString(R.string.introduce_mail_field)
                            0 -> response = getString(R.string.register)
                            1 -> response = getString(R.string.email_sent)
                        }
                        //Dialogue with result of the operation
                        runOnUiThread {
                            AlertDialog.Builder(context)
                                .setMessage(response)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                }
            }
            // Button to cancel the operation
            builder.setNegativeButton(getString(R.string.cancel)){dialog, _ ->
                dialog.cancel()
            }
            // We show the dialogue
            builder.show()
        }
    }
    private fun moveNext(remember : Boolean){
        intent = Intent(this, SecondActivity::class.java)
        intent.putExtra("remember", remember)
        this.startActivity(intent)
        this.finish()
    }
}