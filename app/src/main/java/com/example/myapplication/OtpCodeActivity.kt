package com.example.myapplication

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.config.AppConfig
import com.example.myapplication.data.config.ConfigLoader
import com.example.myapplication.data.model.OtpRequest
import com.example.myapplication.data.model.OtpVerifyRequest
import com.example.myapplication.data.network.AuthService
import com.example.myapplication.data.network.RetrofitClient
import com.example.myapplication.data.session.SessionManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

class OtpCodeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
        private const val OTP_TIMEOUT_SECONDS = 120L
    }

    private lateinit var otpInput: TextInputEditText
    private lateinit var verifyButton: MaterialButton
    private lateinit var resendButton: MaterialButton
    private lateinit var changeEmailButton: MaterialButton
    private lateinit var errorText: MaterialTextView
    private lateinit var timerText: MaterialTextView
    private lateinit var emailText: MaterialTextView
    private lateinit var verifyProgress: LinearProgressIndicator
    private lateinit var coordinator: View

    private lateinit var email: String
    private lateinit var configLoader: ConfigLoader
    private var appConfig: AppConfig? = null
    private var authService: AuthService? = null
    private lateinit var sessionManager: SessionManager

    private var countDownTimer: CountDownTimer? = null
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_code)

        email = intent.getStringExtra(EXTRA_EMAIL) ?: ""
        if (email.isEmpty()) {
            finish()
            return
        }

        configLoader = ConfigLoader(this)
        sessionManager = SessionManager(this)
        loadConfiguration()
        setupUI()
        setupToolbar()
        startOtpTimer()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.otp_code_toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupUI() {
        coordinator = findViewById(R.id.otp_code_coordinator)
        otpInput = findViewById(R.id.otp_code_input)
        verifyButton = findViewById(R.id.otp_code_verify_button)
        resendButton = findViewById(R.id.otp_code_resend_button)
        changeEmailButton = findViewById(R.id.otp_code_change_email_button)
        errorText = findViewById(R.id.otp_code_error)
        timerText = findViewById(R.id.otp_code_timer)
        emailText = findViewById(R.id.otp_code_email_text)
        verifyProgress = findViewById(R.id.otp_code_verify_progress)

        // Display masked email
        emailText.text = getString(R.string.otp_sent_to_email, maskEmail(email))

        // OTP input validation
        otpInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateOtpLength()
                errorText.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable?) {
                handlePaste()
            }
        })

        // Auto-focus OTP input
        otpInput.requestFocus()

        // Button listeners
        verifyButton.setOnClickListener { onVerifyOtpClick() }
        resendButton.setOnClickListener { onResendOtpClick() }
        changeEmailButton.setOnClickListener { onChangeEmailClick() }
    }

    private fun validateOtpLength(): Boolean {
        val otp = otpInput.text.toString().trim()
        val isValid = otp.length == 6 && otp.all { it.isDigit() }
        verifyButton.isEnabled = isValid
        return isValid
    }

    private fun handlePaste() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip?.getItemAt(0)
        val pastedText = clip?.text?.toString() ?: return

        if (pastedText.length == 6 && pastedText.all { it.isDigit() }) {
            otpInput.setText(pastedText)
            otpInput.setSelection(pastedText.length)
        }
    }

    private fun onVerifyOtpClick() {
        val otp = otpInput.text.toString().trim()

        if (otp.length != 6) {
            showError(getString(R.string.error_invalid_otp))
            return
        }

        setVerifying(true)

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

                val request = OtpVerifyRequest(email, otp)
                val response = service.verifyOtp(config.otpVerifyEndpoint, request).execute()

                runOnUiThread {
                    setVerifying(false)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val token = body.token?.trim()
                        val userId = body.userId?.trim()
                        if (!token.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                            sessionManager.saveSession(token, userId, email)
                            navigateToHome()
                        } else {
                            showError(getString(R.string.otp_verify_generic))
                        }
                    } else {
                        val errorMsg = when (response.code()) {
                            401 -> getString(R.string.otp_incorrect)
                            410 -> getString(R.string.otp_expired_resend)
                            else -> getString(R.string.otp_verify_generic)
                        }
                        showError(errorMsg)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setVerifying(false)
                    showError(getString(R.string.otp_verify_generic))
                }
            }
        }
    }

    private fun onResendOtpClick() {
        setResending(true)

        executorService.execute {
            try {
                val service = authService ?: run {
                    runOnUiThread { setResending(false) }
                    return@execute
                }

                val config = appConfig ?: run {
                    runOnUiThread { setResending(false) }
                    return@execute
                }

                val request = OtpRequest(email)
                val response = service.requestOtp(config.otpRequestEndpoint, request).execute()

                runOnUiThread {
                    setResending(false)
                    if (response.isSuccessful) {
                        showInfo(getString(R.string.otp_resent_success))
                        startOtpTimer()
                    } else {
                        showError(getString(R.string.otp_request_failed_generic))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setResending(false)
                    showError(getString(R.string.otp_request_failed_generic))
                }
            }
        }
    }

    private fun onChangeEmailClick() {
        finish()
    }

    private fun startOtpTimer() {
        countDownTimer?.cancel()
        resendButton.isEnabled = false

        countDownTimer = object : CountDownTimer((OTP_TIMEOUT_SECONDS * 1000), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = max(0, millisUntilFinished / 1000)
                val minutes = seconds / 60
                val secondsRem = seconds % 60
                timerText.text = getString(
                    R.string.otp_timer_format,
                    String.format("%02d:%02d", minutes, secondsRem)
                )
            }

            override fun onFinish() {
                timerText.text = getString(R.string.otp_expired)
                resendButton.isEnabled = true
            }
        }.start()
    }

    private fun setVerifying(isVerifying: Boolean) {
        verifyButton.isEnabled = !isVerifying
        otpInput.isEnabled = !isVerifying
        verifyProgress.visibility = if (isVerifying) View.VISIBLE else View.GONE
    }

    private fun setResending(isResending: Boolean) {
        resendButton.isEnabled = !isResending
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun showInfo(message: String) {
        Snackbar.make(coordinator, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val domain = parts[1]
        return when {
            name.length <= 3 -> "${name.firstOrNull() ?: '*'}***@$domain"
            else -> "${name.take(3)}***@$domain"
        }
    }

    private fun loadConfiguration() {
        appConfig = configLoader.loadConfig()
        appConfig?.let { config ->
            authService = RetrofitClient.getClient(config).create(AuthService::class.java)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        executorService.shutdown()
    }
}

