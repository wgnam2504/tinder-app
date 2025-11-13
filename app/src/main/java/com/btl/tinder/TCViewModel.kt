package com.btl.tinder

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.btl.tinder.data.COLLECTION_CHAT
import com.btl.tinder.data.COLLECTION_USER
import com.btl.tinder.data.ChatData
import com.btl.tinder.data.ChatUser
import com.btl.tinder.data.Event
import com.btl.tinder.data.UserData
import com.btl.tinder.ui.Gender
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TCViewModel @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseFirestore,
    val storage: FirebaseStorage
): ViewModel() {

    val inProgress = mutableStateOf(false)
    val popupNotification = mutableStateOf<Event<String>?>(null)
    val signedIn = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)

    val matchProfiles = mutableStateOf<List<UserData>>(listOf())
    val inProgressProfiles = mutableStateOf(false)

    init {
        //auth.signOut()
        val currentUser = auth.currentUser
        signedIn.value = currentUser != null
        currentUser?.uid?.let { uid ->
            getUserData(uid)
        }
    }

    fun onSignup(username: String, email: String, pass: String, navController: NavController) {
        if (username.isEmpty() or email.isEmpty() or pass.isEmpty()) {
            handleException(customMessage = "Please fill in all fields")
            return
        }
        inProgress.value = true
        db.collection(COLLECTION_USER).whereEqualTo("username", username)
            .get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    auth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener {task ->
                            if (task.isSuccessful) {
                                createOrUpdateProfile(username = username)
                                navController.navigate(DestinationScreen.Login.route)
                            }

                            if (task.isSuccessful){
                                signedIn.value = true
                                createOrUpdateProfile(username = username)
                            }


                            else
                                handleException(task.exception, "Signup failed")
                        }
                }
                else {
                    handleException(customMessage = "Username already exists")
                }
                inProgress.value = false
            }
            .addOnFailureListener {
                handleException(it)
            }
    }

    fun onLogin(email: String, pass: String) {
        if (email.isEmpty() or pass.isEmpty()) {
            handleException(customMessage = "Please fill in all fields")
            return
        }
        inProgress.value = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    signedIn.value = true
                    inProgress.value = false
                    auth.currentUser?.uid?.let {
                        getUserData(it)
                    }
                } else
                    handleException(task.exception, "Login failed")
            }
            .addOnFailureListener {
                handleException(it, "Login failed")
            }
    }

    private fun createOrUpdateProfile(
        name: String? = null,
        username: String? = null,
        bio: String? = null,
        imageUrl: String? = null,
        gender: Gender? = null,
        genderPreference: Gender? = null
    ) {
        val uid = auth.currentUser?.uid
        val userData = UserData(
            userId = uid,
            name = name ?: userData.value?.name,
            username = username ?: userData.value?.username,
            imageUrl = imageUrl ?: userData.value?.imageUrl,
            bio = bio ?: userData.value?.bio,
            gender = gender?.toString() ?: userData.value?.gender,
            genderPreference = genderPreference?.toString() ?: userData.value?.genderPreference
        )

        uid?.let {
            inProgress.value = true
            db.collection(COLLECTION_USER).document(uid)
                .get()
                .addOnSuccessListener {
                    if (it.exists()) //Update
                        it.reference.update(userData.toMap())
                            .addOnSuccessListener {
                                this.userData.value = userData
                                inProgress.value = false
                                popupNotification.value = Event("Profile updated")
                            }
                            .addOnFailureListener { it ->
                                handleException(it, "Cannot update user")
                                inProgress.value = false
                            }
                    else { //Create new
                        db.collection(COLLECTION_USER).document(uid).set(userData)
                        inProgress.value = false
                        getUserData(uid)
                    }
                }
                .addOnFailureListener {
                    handleException(it, "Cannot create user")
                    inProgress.value = false
                }
        }
    }

    private fun getUserData(uid: String) {
        inProgress.value = true
        db.collection(COLLECTION_USER).document(uid)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("TCViewModel", "Error getting user data: ${error.message}")
                    handleException(error, "Cannot get user data")
                }
                if (value != null) {
                    val user = value.toObject(UserData::class.java)
                    userData.value = user
                    inProgress.value = false
                    Log.d("TCViewModel", "User data loaded: ${userData.value}")
                    populateCards()
                }
            }
    }

    fun onLogout() {
        auth.signOut()
        signedIn.value = false
        userData.value = null
        popupNotification.value = Event("Logged out")
    }
    fun updateProfileData(
        name: String,
        username: String,
        bio: String,
        gender: Gender,
        genderPreference: Gender
    ){
        createOrUpdateProfile(
            name = name,
            username = username,
            bio = bio,
            gender = gender,
            genderPreference = genderPreference
        )
    }
    private fun uploadImage(uri: Uri, onImageUploaded: (Uri) -> Unit){
        inProgress.value = true
        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("images/$uuid")
        val uploadTask = imageRef.putFile(uri)
        uploadTask
            .addOnSuccessListener {
                val res = it.metadata?.reference?.downloadUrl
                res?.addOnSuccessListener(onImageUploaded)
            }
            .addOnFailureListener {
                handleException(it)
                inProgress.value = false

            }
    }
    fun uploadProfileImage(uri: Uri){
        uploadImage(uri){
            createOrUpdateProfile(imageUrl = it.toString())
        }
    }

    private fun handleException(exception: Exception? = null, customMessage: String = "") {
        Log.e("LoveMatch", "Exception", exception)
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isEmpty()) errorMsg else "$customMessage: $errorMsg"
        popupNotification.value = Event(message)
        inProgress.value = false
    }

    private fun populateCards1() {
        inProgressProfiles.value = true

        val g = if (userData.value?.gender.isNullOrEmpty()) "ANY" else userData.value!!.gender!!.uppercase()
        val gPref = if (userData.value?.genderPreference.isNullOrEmpty()) "ANY" else userData.value!!.genderPreference!!.uppercase()

        val cardsQuery = when (Gender.valueOf(gPref)) {
            Gender.MALE -> db.collection(COLLECTION_USER).whereEqualTo("gender", Gender.MALE)
            Gender.FEMALE -> db.collection(COLLECTION_USER).whereEqualTo("gender", Gender.FEMALE)
            Gender.ANY -> db.collection(COLLECTION_USER)
        }
        val userGender = Gender.valueOf(g)


        cardsQuery.where(
            com.google.firebase.firestore.Filter.and(
                com.google.firebase.firestore.Filter.notEqualTo("userId", userData.value?.userId),
                com.google.firebase.firestore.Filter.or(
                    com.google.firebase.firestore.Filter.equalTo("genderPreference", userGender),
                    com.google.firebase.firestore.Filter.equalTo("genderPreference", Gender.ANY)
                )
            )
        )
    }

    private fun populateCards() {
        inProgressProfiles.value = true
        Log.d("TCViewModel", "populateCards called. Current User Data: ${userData.value}")

        val g = if (userData.value?.gender.isNullOrEmpty()) "ANY"
        else userData.value!!.gender!!.uppercase()
        val gPref = if (userData.value?.genderPreference.isNullOrEmpty()) "ANY"
        else userData.value!!.genderPreference!!.uppercase()

        Log.d("TCViewModel", "User Gender: $g, Preference: $gPref")

        val cardsQuery =
            when (Gender.valueOf(gPref)) {
                Gender.MALE -> db.collection(COLLECTION_USER)
                    .whereEqualTo("gender", Gender.MALE)
                Gender.FEMALE -> db.collection(COLLECTION_USER)
                    .whereEqualTo("gender", Gender.FEMALE)
                Gender.ANY -> db.collection(COLLECTION_USER)
            }
        val userGender = Gender.valueOf(g)

        cardsQuery.where(
            com.google.firebase.firestore.Filter.and(
                com.google.firebase.firestore.Filter.notEqualTo("userId", userData.value?.userId),
                com.google.firebase.firestore.Filter.or(
                    com.google.firebase.firestore.Filter.equalTo("genderPreference", userGender),
                    com.google.firebase.firestore.Filter.equalTo("genderPreference", Gender.ANY)
                )
            )
        )
            .addSnapshotListener { value, error ->
                if (error != null) {
                    inProgressProfiles.value = false
                    Log.e("TCViewModel", "Error fetching cards: ${error.message}", error)
                    handleException(error)
                }
                if (value != null) {
                    Log.d("TCViewModel", "Fetched ${value.documents.size} documents from Firestore.")
                    val potentials = mutableListOf<UserData>()
                    value.documents.forEach {
                        it.toObject<UserData>()?.let {potential ->
                            var showUser = true
                            Log.d("TCViewModel", "Processing potential user: ${potential.userId}, Name: ${potential.name}")
                            if (
                                userData.value?.swipesLeft?.contains(potential.userId) == true ||
                                userData.value?.swipesRight?.contains(potential.userId) == true ||
                                userData.value?.matches?.contains(potential.userId) == true
                            ) {
                                showUser = false
                                Log.d("TCViewModel", "User ${potential.userId} already swiped/matched. Not showing.")
                            }
                            if (showUser)
                                potentials.add(potential)
                        }
                    }

                    Log.d("TCViewModel", "Found ${potentials.size} potential matches after filtering.")
                    matchProfiles.value = potentials
                    inProgressProfiles.value = false
                }
            }
    }

    fun onDislike(selectedUser: UserData) {
        db.collection(COLLECTION_USER).document(userData.value?.userId ?: "")
            .update("swipesLeft", FieldValue.arrayUnion(selectedUser.userId))
    }

    fun onLike(selectedUser: UserData) {
        // Gốc ko có non-null
        val reciprocalMatch = selectedUser.swipesRight?.contains(userData.value?.userId)
        if (!reciprocalMatch!!) {
            db.collection(COLLECTION_USER).document(userData.value?.userId ?: "")
                .update("swipesRight", FieldValue.arrayUnion(selectedUser.userId))
        } else {
            popupNotification.value = Event("Match!")

            db.collection(COLLECTION_USER).document(selectedUser.userId ?: "")
                .update("swipesRight", FieldValue.arrayRemove(userData.value?.userId))
            db.collection(COLLECTION_USER).document(selectedUser.userId ?: "")
                .update("matches", FieldValue.arrayUnion(userData.value?.userId))
            db.collection(COLLECTION_USER).document(userData.value?.userId ?: "")
                .update("matches", FieldValue.arrayUnion(selectedUser.userId))

            val chatKey = db.collection(COLLECTION_CHAT).document().id
            val chatData = ChatData(
                chatKey,
                ChatUser(
                    userData.value?.userId,
                    if (userData.value?.name.isNullOrEmpty()) userData.value?.username
                    else userData.value?.name,
                    userData.value?.imageUrl
                ),
                ChatUser(
                    selectedUser.userId,
                    if (selectedUser.name.isNullOrEmpty()) selectedUser.username
                    else selectedUser.name,
                    selectedUser.imageUrl
                )
            )
            db.collection(COLLECTION_CHAT).document(chatKey).set(chatData)
        }
    }

    fun getStreamToken(userId: String, onComplete: (String) -> Unit) {
        // Lấy instance đúng cách
        val functions: FirebaseFunctions = Firebase.functions

        val data = hashMapOf("userId" to userId)

        functions
            .getHttpsCallable("getStreamToken")
            .call(data)
            .continueWith { task ->
                // Lấy kết quả dưới dạng Map
                val result = task.result?.data as? Map<String, Any>
                result?.get("token") as? String
            }
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Xử lý lỗi
                    handleException(task.exception, "Could not get Stream token.")
                } else {
                    val token = task.result
                    if (token != null) {
                        // Lấy token thành công
                        onComplete(token)
                    }
                }
            }
    }

}

