package com.example.prueba

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.prueba.firebase.getCurrentUser
import com.example.prueba.firebase.logIn
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
        if (getCurrentUser() != null)
            moveNext(true)
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
            CoroutineScope(Dispatchers.IO).launch {
                if(logIn(user.text.toString(), password.text.toString()) == true) {
                    //If successful, we'll move to the next activity
                    moveNext(remember.isActivated)
                } else {
                    //Otherwise we'll show an error message to the user
                    runOnUiThread {
                        password.text.clear()
                        wrong.isVisible = true
                    }
                }
            }
        }
    }
    private fun moveNext(remember : Boolean){
        intent = Intent(this, SecondActivity::class.java)
        intent.putExtra("remember", remember)
        this.startActivity(intent)
    }
}