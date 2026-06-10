package com.example.foragingapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.R
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.databinding.ActivityProfileBinding
import com.example.foragingapp.model.LogEntry
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonEditProfile.setOnClickListener { showEditProfileDialog() }
        binding.buttonSignOut.setOnClickListener {
            AuthManager.signOut(this)
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
            renderUser()
        }
        binding.buttonPrivacyPolicy.setOnClickListener {
            showInfoDialog("Privacy Policy", getString(R.string.privacy_policy_summary))
        }
        binding.buttonAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        binding.buttonTerms.setOnClickListener {
            showInfoDialog("Terms of Service", getString(R.string.terms_summary))
        }

        renderUser()
        viewModel.allLogs.observe(this) { logs -> renderTrees(logs) }
    }

    private fun renderUser() {
        val user = AuthManager.currentUser(this)
        if (user == null) {
            binding.textName.text = "Guest"
            binding.textEmail.text = "Sign in to contribute discoveries."
            binding.textCreated.text = "Account information unavailable"
            binding.buttonEditProfile.text = "Sign In"
            binding.buttonEditProfile.setOnClickListener { startActivity(Intent(this, AuthActivity::class.java)) }
            binding.buttonSignOut.visibility = View.GONE
            binding.guestOnboarding.visibility = View.VISIBLE
        } else {
            binding.textName.text = user.fullName
            binding.textEmail.text = user.email
            binding.textCreated.text = "Account created: ${user.createdAt.ifBlank { "Unknown" }}"
            binding.buttonEditProfile.text = "Edit Profile"
            binding.buttonEditProfile.setOnClickListener { showEditProfileDialog() }
            binding.buttonSignOut.visibility = View.VISIBLE
            binding.guestOnboarding.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        renderUser()
    }

    private fun renderTrees(logs: List<LogEntry>) {
        val user = AuthManager.currentUser(this)
        val myTrees = if (user == null) emptyList() else logs.filter { it.creatorUserId == user.id }
        val savedTrees = if (user == null) emptyList() else logs.filter { it.isFavorite }
        renderTreeList(binding.myTreesContainer, myTrees, "Your discoveries will appear here.")
        renderTreeList(binding.savedTreesContainer, savedTrees, "Saved fruit trees will appear here.")
    }

    private fun renderTreeList(container: LinearLayout, trees: List<LogEntry>, emptyText: String) {
        container.removeAllViews()
        if (trees.isEmpty()) {
            val empty = TextView(this)
            empty.text = emptyText
            empty.setTextColor(getColor(com.example.foragingapp.R.color.textSecondary))
            empty.setPadding(16, 16, 16, 16)
            container.addView(empty)
            return
        }
        trees.forEach { tree -> container.addView(treeCard(tree)) }
    }

    private fun treeCard(tree: LogEntry): View {
        val card = CardView(this)
        card.radius = 20f
        card.cardElevation = 0f
        card.setCardBackgroundColor(getColor(com.example.foragingapp.R.color.surface))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(16, 16, 16, 16)

        val image = ImageView(this)
        image.layoutParams = LinearLayout.LayoutParams(72, 72)
        image.scaleType = ImageView.ScaleType.CENTER_CROP
        if (tree.imageUri.isNotBlank()) Glide.with(this).load(Uri.parse(tree.imageUri)).centerCrop().into(image)

        val text = TextView(this)
        text.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = 16
        }
        text.text = "${tree.name}\n${tree.date.ifBlank { tree.createdAt }}"
        text.setTextColor(getColor(com.example.foragingapp.R.color.textPrimary))
        text.textSize = 15f

        row.addView(image)
        row.addView(text)
        card.addView(row)
        card.setOnClickListener {
            startActivity(Intent(this, TreeDetailsActivity::class.java).putExtra("LOG_ID", tree.id))
        }
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }
        return card
    }

    private fun showEditProfileDialog() {
        val user = AuthManager.currentUser(this)
        if (user == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            return
        }

        val form = LinearLayout(this)
        form.orientation = LinearLayout.VERTICAL
        form.setPadding(32, 8, 32, 0)
        val name = EditText(this).apply { hint = "Full name"; setText(user.fullName) }
        val email = EditText(this).apply { hint = "Email"; setText(user.email) }
        val password = EditText(this).apply { hint = "New password (optional)" }
        form.addView(name)
        form.addView(email)
        form.addView(password)

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(form)
            .setPositiveButton("Save") { _, _ ->
                AuthManager.updateProfile(this, name.text.toString(), email.text.toString(), password.text.toString())
                renderUser()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
