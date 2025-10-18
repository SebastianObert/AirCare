package com.example.aircare

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)


        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, AuthActivity::class.java))
        }

        finish()
    }
}