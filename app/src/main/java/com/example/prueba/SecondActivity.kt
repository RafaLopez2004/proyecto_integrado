package com.example.prueba

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.prueba.firebase.checkIn
import com.example.prueba.firebase.checkLocation
import com.example.prueba.firebase.checkOut
import com.example.prueba.firebase.logOut
import com.example.prueba.firebase.setCurrentUserDocument
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SecondActivity  : AppCompatActivity(){

    private val LOC_CODE = 1000
    private val LOC_CODE_OUT = 1001
    private var safe : Boolean = true
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var context : Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.prueba)
        val inButton: TextView = findViewById(R.id.checkin)
        val outButton: TextView = findViewById(R.id.checkout)
        val menu: ImageButton = findViewById(R.id.superior_menu)
        val logOut: TextView = findViewById(R.id.logout)
        var operating = false
        context = this

        CoroutineScope(Dispatchers.IO).launch {
            setCurrentUserDocument()
        }

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
                Toast.makeText(context, "Wait until the previous operation is completed", Toast.LENGTH_LONG).show()
        }
        // Menu
        menu.setOnClickListener { v: View -> showMenu(v, R.menu.popup_menu) }
    }
    
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

    /**
     * Operations that the popup menu do
     */
    private fun showMenu(v: View, menuRes: Int) {
        val popup = PopupMenu(context, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when(menuItem.itemId){
                R.id.log_out -> logOut()
            }
            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (intent.extras?.getBoolean("remember") == false)
            logOut()
    }

    /*
    * Operations that we'll do when a Permission is granted
    * */
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