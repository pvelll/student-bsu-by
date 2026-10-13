package github.sushkpavel.studentbsuby.services.firebase

import android.content.Context
import com.google.firebase.FirebaseApp

fun initFirebase(context: Context) {
    kotlin.runCatching {
        FirebaseApp.initializeApp(context)
    }
}
