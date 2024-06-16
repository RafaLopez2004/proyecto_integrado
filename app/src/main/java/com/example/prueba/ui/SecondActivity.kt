package com.example.prueba.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prueba.R
import com.example.prueba.firebase.checkIn
import com.example.prueba.firebase.checkLocation
import com.example.prueba.firebase.checkOut
import com.example.prueba.firebase.forgotPass
import com.example.prueba.firebase.formatDate
import com.example.prueba.firebase.getCheckCollection
import com.example.prueba.firebase.getCurrentUser
import com.example.prueba.firebase.getCurrentUserName
import com.example.prueba.firebase.getServerTime
import com.example.prueba.firebase.logOut
import com.example.prueba.firebase.setCurrentUserName
import com.example.prueba.firebase.setCurrentUserDocument
import com.example.prueba.recycler.DataModel
import com.example.prueba.recycler.RecyclerAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.ArrayList

class SecondActivity  : AppCompatActivity(){

    private val LOC_CODE = 1000
    private val LOC_CODE_OUT = 1001
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var context : Context
    private lateinit var mRecyclerView : RecyclerView
    private val mAdapter : RecyclerAdapter = RecyclerAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second_activity)
        setCurrentUserName()
        findViewById<TextView>(R.id.bar_txt).text = String.format(getString(R.string.history), getCurrentUserName())
        context = this
        val inButton: TextView = findViewById(R.id.checkin)
        val outButton: TextView = findViewById(R.id.checkout)
        val menu: ImageButton = findViewById(R.id.superior_menu)
        var operating = false

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
                Toast.makeText(context, R.string.wait, Toast.LENGTH_LONG).show()
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
                Toast.makeText(context, R.string.wait, Toast.LENGTH_LONG).show()
        }
        //Recycler view with history
        this.setUpRecyclerView()
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
            when (checkIn(formatDate(getServerTime()))) {
                1 ->
                    toastText = getString(R.string.check_in_sucess)
                0 ->
                    toastText = getString(R.string.check_in_done)
                -1 ->
                    toastText = getString(R.string.check_in_error)
            } }
        else
            toastText = getString(R.string.check_in_range)
        // Print the result of the operation
        runOnUiThread { Toast.makeText(context, toastText, Toast.LENGTH_LONG).show() }
    }

    private suspend fun checkOutLocal(context: Context){
        var toastText = ""
        val location = getLocation()
        // We check if we are in range to be able to to do the check out
        if (location != null && checkLocation(location)) {
            when (checkOut(formatDate(getServerTime()))) {
                1 ->
                    toastText = getString(R.string.check_out_sucess)
                0 ->
                    toastText = getString(R.string.check_in_out_done)
                -1 ->
                    toastText = getString(R.string.check_out_error)
            } }
        else
            toastText = getString(R.string.check_out_range)
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

    // POP UP MENU RELATED
    /**
     * Operations that the popup menu do
     */
    private fun showMenu(v: View, menuRes: Int) {
        val popup = PopupMenu(context, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            when(menuItem.itemId){
                R.id.log_out -> {
                    var dialog : AlertDialog? = null
                    val builder = AlertDialog.Builder(context)
                    builder.setTitle(getString(R.string.sure))
                    val inflatedView = LayoutInflater.from(context)
                        .inflate(R.layout.log_out_dialog, findViewById(R.id.second), false)
                    val logOutBtn : MaterialButton = inflatedView.findViewById(R.id.log_out_btn_dialog)
                    val cancel : MaterialButton = inflatedView.findViewById(R.id.cancel_button)

                    logOutBtn.setOnClickListener {
                        logOut()
                        this.startActivity(Intent(this, MainActivity::class.java))
                        this.finish()
                    }

                    cancel.setOnClickListener {
                        dialog?.dismiss()
                    }
                    builder.setView(inflatedView)
                    // We show the dialogue
                    dialog = builder.show()
                }
                R.id.remember_menu -> {
                    var response = ""
                    CoroutineScope(Dispatchers.IO).launch {
                        when (forgotPass(getCurrentUser()?.email.toString())) {
                            -1 or 0 -> response = getString(R.string.error)
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
            true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }
    //RECYCLER VIEW FUNCTIONS

    /**
     *
     */
    private fun setUpRecyclerView(){
        mRecyclerView = findViewById(R.id.recycler)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mAdapter.setUp(this)
        CoroutineScope(Dispatchers.IO).launch {
            updateRecycler()
            runOnUiThread { mRecyclerView.adapter = mAdapter }
        }

    }

    private fun updateRecycler(){
        setCurrentUserDocument()
        val collection = getCheckCollection()
        if (collection != null) {
            var util: String?
            var array: MutableList<DataModel>  = ArrayList()
            for (document in collection) {
                var chkIn = getString(R.string.not_done)
                var chkOut =  getString(R.string.not_done)
                util = document.getString("in")
                if (!util.isNullOrEmpty())
                    chkIn = util
                util = document.getString("out")
                if (!util.isNullOrEmpty())
                    chkOut = util
                array.add(DataModel(document.id, chkIn, chkOut))
                //mAdapter.notifyItemInserted(mAdapter.itemCount)
            }
            array = array.sortedWith { a, b ->
                val aSeparated = a.day.split("-")
                val bSeparated = b.day.split("-")
                when {
                    (a.day == b.day) -> 0
                    (aSeparated[2].toInt() < bSeparated[2].toInt()) -> 1
                    (aSeparated[2].toInt() == bSeparated[2].toInt() &&
                            aSeparated[1].toInt() < bSeparated[1].toInt()) -> 1
                    (aSeparated[2].toInt() == bSeparated[2].toInt() &&
                            aSeparated[1].toInt() == bSeparated[1].toInt() &&
                            aSeparated[0].toInt() < bSeparated[0].toInt()) -> 1

                    else -> -1
                }
            }.toMutableList()
            mAdapter.setArrayData(array)
        }
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
        val toastText = getString(R.string.check_workplace)
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