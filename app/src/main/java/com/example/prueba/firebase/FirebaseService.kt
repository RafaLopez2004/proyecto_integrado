package com.example.prueba.firebase

import android.location.Location
import com.example.prueba.time.TimeModel
import com.example.prueba.time.TimeService
import com.google.android.gms.tasks.Tasks.await
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CompletableDeferred
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.collections.HashMap

var userDocument : DocumentSnapshot? = null
var userName : String = ""

/**
 * Returns the currently logged in user, if there isn't any user logged in,
 * it will return null
 */
fun getCurrentUser() : FirebaseUser?{
    return Firebase.auth.currentUser
}

fun setCurrentUserName() {
    if (userName != ""){
        val email = getCurrentUser()?.email
        if (email != null)
            userName = email.split("@")[0]
    }
}

fun getCurrentUserName() : String{
    return userName
}

/**
 * Logs the current user out
 */
fun logOut(){
    userName = ""
    userDocument = null
    Firebase.auth.signOut()
}


// Asynchronous functions

/**
 * Logs in a user an returs a boolean to indicate if it was succesfull
 */

suspend fun logIn(user : String, pass : String) : Boolean? {
    val def = CompletableDeferred<Boolean?>()
    Firebase.auth.signInWithEmailAndPassword(user,pass).addOnCompleteListener {
        if (it.isSuccessful) {
            userName = user.split("@")[0]
            def.complete(true)
        } else
            def.complete(false)
    }
    return def.await()
}
/**
 * Register the check in the database for the current user if he hasn't checked in today already
 */
suspend fun checkIn (documentName: String) : Int?{
    val def = CompletableDeferred<Int?>()
    // We get the current server time
    val serverTime = getServerTime()
    // We wait until we fetch the needed resource from the database
    // We build the path where we'll be adding the data
    val fullPath = "users/" + userDocument!!.id + "/checks"
    val db = Firebase.firestore
    // We check if the document already exists, this would mean the user already checked in
    var exist = pathExists(fullPath, documentName)
    if (exist == 0){
        // The data we'll be adding to the database
        val data = HashMap<String, Any>()
        data["in"] = formatTime(serverTime)
        data["out"] = ""
        // We try to add the data to the databasse
        db.collection(fullPath).document(documentName).set(data).addOnCompleteListener{
            def.complete(if (it.isSuccessful) 1 else -1)
        }
    } else{
        val out = await(db.collection(fullPath).document(documentName).get()).get("out").toString()
        if (out.isEmpty())
            def.complete(if (exist == 1) 0 else -1)
        else {
            val newDocumentName = "$documentName- (2nd turn)"
            exist = pathExists(fullPath, newDocumentName)
            if (exist == 0)
                def.complete(checkIn(newDocumentName))

            def.complete(0)
        }
    }

    return def.await()
}
/**
 *
 */
suspend fun checkOut(documentName: String) : Int? {
    val def = CompletableDeferred<Int?>()
    // We get the current server time
    val serverTime = getServerTime()
    // We wait until we fetch the needed resource from the database
    // We build the path where we'll be adding the data
    val fullPath = "users/" + userDocument!!.id + "/checks"
    // We check if the document already exists, this would mean the user already checked in
    val exist = pathExists(fullPath, documentName)
    val db = Firebase.firestore
    if (exist == 1){
        val collection = db.collection(fullPath)
        //We check if current document check out is done
        if (await(collection.document(documentName).get()).getString("out")?.isEmpty() == true){
            // We try to add the data to the databasse
            collection.document(documentName).update("out",
                formatTime(getServerTime())).addOnCompleteListener{
                def.complete(if (it.isSuccessful) 1 else -1)
            }
        } else if (documentName == formatDate(serverTime))
            def.complete(checkOut("$documentName- (2nd turn)"))
        else def.complete(-2)

    } else def.complete(exist)

    return def.await()
}
/**
 *
 */
fun getCheckCollection() : QuerySnapshot?{ return await(Firebase.firestore.collection(
    "users/" + userDocument!!.id + "/checks").get()) }

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
fun getCurrentUserDocument(): DocumentSnapshot? {
    val db = Firebase.firestore
    return await(db.collection("users")
        .whereEqualTo("email",getCurrentUser()?.email).get())
        .documents
        .first()
}

fun setCurrentUserDocument() {
    val db = Firebase.firestore
    userDocument = await(db.collection("users")
        .whereEqualTo("email",getCurrentUser()?.email).get())
        .documents
        .first()
}

/**
 * Checks if the given location is in range to do the check in
 */
suspend fun checkLocation(location : Location) : Boolean{
    val db = Firebase.firestore
    val workplace = Location("")
    val geoPoint = await(db.collection("workplace")
        .document(getUserWorkplace())
        .get())

    val latitude = geoPoint.getGeoPoint("cords")?.latitude
    val longitude = geoPoint.getGeoPoint("cords")?.longitude
    val range = geoPoint.getDouble("range")

    if (longitude != null && latitude != null && range != null) {
        workplace.longitude = longitude
        workplace.latitude = latitude
        return location.distanceTo(workplace) < range
    }
    else
        return false
}
/**
 * Check the current user's workplace
 */
suspend fun getUserWorkplace() : String {
    val def = CompletableDeferred<String>()
    val workplace = userDocument!!.getString("userWorkplace")
    if(workplace.isNullOrEmpty())
        def.complete("default")
    else
        def.complete(workplace)
    return def.await()
}
/**
 * Returns the time of the server
 */
public suspend fun getServerTime(): TimeModel {
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
public fun formatDate(timeModel: TimeModel) : String {
    return "${timeModel.day}-${timeModel.month}-${timeModel.year}"
}
public fun formatTime(timeModel: TimeModel) : String{
    return "${timeModel.hour}:${timeModel.minute}"
}
