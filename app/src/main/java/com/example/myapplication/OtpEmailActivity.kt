package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.config.AppConfig
import com.example.myapplication.data.config.ConfigLoader
import com.example.myapplication.data.model.OtpRequest
import com.example.myapplication.data.network.AuthService
import com.example.myapplication.data.network.RetrofitClient
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OtpEmailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var emailInput: TextInputEditText
    private lateinit var sendButton: MaterialButton
    private lateinit var errorText: MaterialTextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var coordinator: View

    private lateinit var configLoader: ConfigLoader
    private var appConfig: AppConfig? = null
    private var authService: AuthService? = null

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_email)

        configLoader = ConfigLoader(this)
        loadConfiguration()
        setupUI()
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.otp_email_toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUI() {
        coordinator = findViewById(R.id.otp_email_coordinator)
        emailInput = findViewById(R.id.otp_email_input)
        sendButton = findViewById(R.id.otp_email_send_button)
        errorText = findViewById(R.id.otp_email_error)
        progressIndicator = findViewById(R.id.otp_email_progress)

        // Email validation on text change
        emailInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateEmail()
                errorText.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sendButton.setOnClickListener { onSendOtpClick() }
    }

    private fun validateEmail(): Boolean {
        val email = emailInput.text.toString().trim()
        val isValid = email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        sendButton.isEnabled = isValid
        return isValid
    }

    private fun onSendOtpClick() {
        val email = emailInput.text.toString().trim()

        if (!validateEmail()) {
            showError(getString(R.string.error_invalid_email))
            return
        }

        setLoading(true)

        executorService.execute {
            try {
                val service = authService ?: run {
                    runOnUiThread { showError(getString(R.string.error_config_load)) }
                    return@execute
                }

                val config = appConfig ?: run {
                    runOnUiThread { showError(getString(R.string.error_config_load)) }
                    return@execute
                }

                val request = OtpRequest(email)
                val response = service.requestOtp(config.otpRequestEndpoint, request).execute()

                runOnUiThread {
                    setLoading(false)
                    if (response.isSuccessful) {
                        // Navigate to OTP Code screen
                        val intent = Intent(this, OtpCodeActivity::class.java)
                        intent.putExtra(OtpCodeActivity.EXTRA_EMAIL, email)
                        startActivity(intent)
                    } else {
                        showError(getString(R.string.otp_request_failed_generic))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setLoading(false)
                    showError(getString(R.string.otp_request_failed_generic))
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        sendButton.isEnabled = !isLoading
        emailInput.isEnabled = !isLoading
        progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun loadConfiguration() {
        appConfig = configLoader.loadConfig()
        appConfig?.let { config ->
            authService = RetrofitClient.getClient(config).create(AuthService::class.java)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdown()
    }
}


