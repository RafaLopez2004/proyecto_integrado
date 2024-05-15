package com.example.prueba.firebase

import android.location.Location
import com.example.prueba.time.TimeModel
import com.example.prueba.time.TimeService
import com.google.android.gms.tasks.Tasks.await
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CompletableDeferred
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.collections.HashMap


/**
 * Returns the currently logged in user, if there isn't any user logged in,
 * it will return null
 */
fun getCurrentUser() : FirebaseUser?{
    return Firebase.auth.currentUser
}

/**
 * Logs the current user out
 */
fun logOut(){
    Firebase.auth.signOut()
}


// Asynchronous functions

/**
 * Logs in a user an returs a boolean to indicate if it was succesfull
 */

suspend fun logIn(user : String, pass : String) : Boolean? {
    val def = CompletableDeferred<Boolean?>()
    Firebase.auth.signInWithEmailAndPassword(user,pass).addOnCompleteListener {
        def.complete(it.isSuccessful)
    }
    return def.await()
}
/**
 * Create a new user and returns a integer witch indicates if it was succesful (1),
 * the email is already registered (0) or there was an error (-1)
 */
suspend fun createUser(email : String, pass : String) : Int? {
    val def = CompletableDeferred<Int?>()
    //TODO We check if the user already exist

    // We create a user
    Firebase.auth.createUserWithEmailAndPassword(email,pass).addOnCompleteListener{
        // If the user is created succesfully
        if (it.isSuccessful) {
            /* We add the user to the database */
            val data = HashMap<String, Any>()
            val db = Firebase.firestore
            data["email"] = email
            data["checks"] = HashMap<String, Any>()
            db.collection("users")
                .add(data)
                .addOnCompleteListener {itd->
                    if (itd.isSuccessful){
                        def.complete(1)
                    }else
                        getCurrentUser()?.delete()?.addOnCompleteListener {
                            def.complete(-1)
                        }
                }
        } else
            def.complete(-1)
    }
    return def.await()
}
/**
 * Register the check in the database for the current user if he hasn't checked in today already
 */
suspend fun checkIn (safe : Boolean) : Int?{
    val def = CompletableDeferred<Int?>()
    // We get the current server time
    val serverTime = getServerTime()
    // We wait until we fetch the needed resource from the database
    val userDocument = getCurrentUserDocument()
    // We build the path where we'll be adding the data
    val fullPath = "users/" + userDocument!!.id + "/checks"
    // We check if the document already exists, this would mean the user already checked in
    val exist = pathExists(fullPath, formatDate(serverTime))
    if (exist == 0){
        val db = Firebase.firestore
        // The data we'll be adding to the database
        val data = HashMap<String, Any>()
        data["in"] = formatTime(serverTime)
        data["out"] = ""
        data["safe"] = safe
        // We try to add the data to the databasse
        db.collection(fullPath).document(formatDate(serverTime)).set(data).addOnCompleteListener{
            def.complete(if (it.isSuccessful) 1 else -1)
        }
    }else{
        def.complete(if (exist == 1) 0 else -1)
    }
    return def.await()
}
/**
 *
 */
suspend fun checkOut() : Int? {
    val def = CompletableDeferred<Int?>()
    // We get the current server time
    val serverTime = getServerTime()
    // We wait until we fetch the needed resource from the database
    val userDocument = getCurrentUserDocument()
    // We build the path where we'll be adding the data
    val fullPath = "users/" + userDocument!!.id + "/checks"
    // We check if the document already exists, this would mean the user already checked in
    val exist = pathExists(fullPath, formatDate(serverTime))
    if (exist == 1){
        val db = Firebase.firestore
        // We try to add the data to the databasse
        db.collection(fullPath).document(formatDate(serverTime)).update("out",
            formatTime(getServerTime())).addOnCompleteListener{
            def.complete(if (it.isSuccessful) 1 else -1)
        }
    }else{
        def.complete(exist)
    }
    return def.await()
}
/**
 * Tries to send a reset email to the given email, if it was successful it will return 1,
 * -1 if an error occurred or 0  if the email isn't registered
 */
suspend fun forgotPass(email: String) : Int?{
    val def = CompletableDeferred<Int?>()
    if (!await(Firebase.firestore.collection("users")
            .whereEqualTo("email",email)
            .get()).isEmpty)
        Firebase.auth.sendPasswordResetEmail(email).addOnCompleteListener {
            def.complete(if (it.isSuccessful) 1 else -1)
        }
    else
        def.complete(0)
    return def.await()
}
// Utilities
/**
 * Get the current user's document id from the database
 */
private fun getCurrentUserDocument(): DocumentSnapshot? {
    val db = Firebase.firestore
    return await(db.collection("users")
        .whereEqualTo("email",getCurrentUser()?.email).get())
        .documents
        .first()
}
/**
 * Checks if the given location is in range to do the check in
 */
fun checkLocation(location : Location) : Boolean{
    val db = Firebase.firestore
    val workplace = Location("")
    val latitude = await(db.collection("users").document("cords").get()).getDouble("lat")
    val longitude = await(db.collection("users").document("cords").get()).getDouble("lon")

    if (longitude != null && latitude != null) {
        workplace.longitude = longitude
        workplace.latitude = latitude
        return location.distanceTo(workplace) < 1000 }
    else
        return false
}
/**
 * Returns the time of the server
 */
private suspend fun getServerTime(): TimeModel {
    // Set the URL to the API
    val retrofit = Retrofit.Builder()
        .baseUrl("https://tools.aimylogic.com/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    return retrofit.create(TimeService::class.java).getTime()
}
/**
 * Checks if a document with the specified path exists in the database where -1 an error has
 * ocurred, 0 it does not exist and 1 it exist
 */
private suspend fun pathExists(path : String, documentName : String) : Int? {
    val def = CompletableDeferred<Int?>()
    val db = Firebase.firestore
    db.collection(path).document(documentName).get().addOnCompleteListener {
        //We check if the reference is correct
        if (it.isSuccessful){
            // We check if there is a document created in the reference
            val document = it.result
            if (document != null) {
                if (document.exists())
                    def.complete(1)
                else
                    def.complete(0)
            }
            else
                def.complete(-1)
        } else
            def.complete(-1)
    }
    return def.await()
}
private fun formatDate(timeModel: TimeModel) : String {
    return "${timeModel.day}-${timeModel.month}-${timeModel.year}"
}
private fun formatTime(timeModel: TimeModel) : String{
    return "${timeModel.hour}:${timeModel.minute}:${timeModel.second}"
}
