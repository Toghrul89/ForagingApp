package com.example.foragingapp.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonRegister.setOnClickListener {
            val ok = AuthManager.register(
                this,
                binding.editName.text.toString(),
                binding.editEmail.text.toString(),
                binding.editPassword.text.toString()
            )
            if (ok) finish() else Toast.makeText(this, "Enter name, email, and password.", Toast.LENGTH_SHORT).show()
        }

        binding.buttonLogin.setOnClickListener {
            val ok = AuthManager.login(this, binding.editEmail.text.toString(), binding.editPassword.text.toString())
            if (ok) finish() else Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show()
        }

        binding.buttonGoogle.setOnClickListener {
            Toast.makeText(this, "Google Sign-In needs your Firebase setup first.", Toast.LENGTH_LONG).show()
        }
    }
}
