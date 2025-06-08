package com.example.farmdatapod.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.DBHandler
import com.example.farmdatapod.FarmDataPodApplication
import com.example.farmdatapod.MainActivity
import com.example.farmdatapod.R // Ensure this points to your R file
import com.example.farmdatapod.databinding.ActivityLoginBinding
import com.example.farmdatapod.utils.SharedPrefs // Retained for storing username/password for offline convenience
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.data.DataSyncManager
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.utils.DialogUtils
import com.example.farmdatapod.models.LoginState
import com.example.farmdatapod.sync.SyncManager
import com.example.farmdatapod.utils.TokenManager // Ensure TokenManager is imported
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var dbHandler: DBHandler
    private lateinit var sharedPrefs: SharedPrefs // For username/password form pre-filling
    private lateinit var networkViewModel: NetworkViewModel
    private lateinit var dataSyncManager: DataSyncManager
    private lateinit var syncManager: SyncManager     // From Application
    private lateinit var tokenManager: TokenManager   // From Application
    private lateinit var dialogUtils: DialogUtils
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindow()
        initializeBinding()
        initializeComponents()
        setupObservers()
        setupLoginButton()
        checkAutoLogin()
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun initializeBinding() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initializeComponents() {
        try {
            Log.d(TAG, "Initializing components")

            val farmApp = application as FarmDataPodApplication
            tokenManager = farmApp.tokenManager
            syncManager = farmApp.syncManager
            Log.d(TAG, "Got TokenManager and SyncManager from Application")

            // LoginViewModel is created here.
            // Reminder: LoginViewModel MUST be updated to use farmApp.tokenManager for its token operations.
            viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

            networkViewModel = ViewModelProvider(this)[NetworkViewModel::class.java]
            dbHandler = DBHandler(this)
            sharedPrefs = SharedPrefs(this) // For non-session related preferences like saved username

            // Use the RestClient.getApiService(context) as per your provided RestClient.kt
            // The AuthInterceptor within this RestClient will use its own SharedPrefs instance.
            // Token consistency relies on LoginViewModel (via TokenManager) writing to the same SharedPrefs.
            val apiService = RestClient.getApiService(this) //
            dataSyncManager = DataSyncManager(apiService, sharedPrefs, dbHandler, this)

            dialogUtils = DialogUtils(this)
            loadingDialog = dialogUtils.buildLoadingDialog()

            checkDatabaseStatus()
            Log.d(TAG, "Components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
            showError("Failed to initialize app components: ${e.message}")
        }
    }

    private fun checkDatabaseStatus() {
        try {
            if (!::dbHandler.isInitialized) {
                dbHandler = DBHandler(this)
            }
            dbHandler.readableDatabase.use { db ->
                Log.d(TAG, "Database path: ${db.path}")
                Log.d(TAG, "Database exists: ${db.isOpen}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening database", e)
            showError("Database initialization failed")
        }
    }

    private fun setupObservers() {
        networkViewModel.networkLiveData.observe(this) { isNetworkAvailable ->
            Log.d(TAG, "Network status changed: $isNetworkAvailable")
            updateNetworkStatus(isNetworkAvailable)
        }

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> showLoading("Logging in...")
                is LoginState.Success -> handleSuccessfulLogin()
                is LoginState.Error -> handleLoginError(state.message)
                is LoginState.Offline -> handleOfflineState()
                null -> hideLoading()
            }
        }
    }

    private fun setupLoginButton() {
        binding.loginButton.setOnClickListener {
            Log.d(TAG, "Login button clicked")
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        val email = binding.username.text.toString().trim()
        val password = binding.password.text.toString().trim()

        if (validateFields(email, password)) {
            performLogin(email, password)
        }
    }

    private fun handleLoginError(message: String) {
        Log.e(TAG, "Login error: $message")
        hideLoading()
        binding.loginButton.isEnabled = true
        showError(message)
    }

    private fun handleOfflineState() {
        Log.d(TAG, "Handling offline state (e.g. credentials failed or just informational).")
        hideLoading()
        binding.loginButton.isEnabled = true
        // The original LoginViewModel's offline state handling might need review.
        // For now, just showing a message.
        showError("Offline. Functionality may be limited or using cached credentials if login was successful offline.")
    }

    private fun checkAutoLogin() {
        // viewModel.isUserLoggedIn() MUST be updated in LoginViewModel to use
        // the application-scoped tokenManager.isTokenValid()
        if (viewModel.isUserLoggedIn()) {
            Log.d(TAG, "Auto login: User token is valid (checked via ViewModel).")
            navigateToMainActivity()
        } else {
            Log.d(TAG, "Auto login: No valid token. User needs to login.")
        }
    }

    private fun updateNetworkStatus(isAvailable: Boolean) {
        Log.d(TAG, "Updating network status: $isAvailable")
        binding.networkStatusText?.apply {
            text = if (isAvailable) "Online" else "Offline"
            setTextColor(getColor(
                if (isAvailable) R.color.green_online_status // Define in colors.xml
                else R.color.red_offline_status   // Define in colors.xml
            ))
        }
    }

    private fun validateFields(email: String, password: String): Boolean {
        var isValid = true
        binding.username.error = null
        binding.password.error = null
        if (email.isEmpty()) {
            binding.username.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.username.error = "Invalid email format"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.password.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.password.error = "Password must be at least 6 characters"
            isValid = false
        }
        Log.d(TAG, "Field validation result: $isValid")
        return isValid
    }


    private fun showLoading(message: String = "Loading...") {
        Log.d(TAG, "Showing loading dialog: $message")
        if (!isFinishing) {
            loadingDialog?.setMessage(message)
            if (loadingDialog?.isShowing == false) {
                loadingDialog?.show()
            }
        }
    }

    private fun hideLoading() {
        Log.d(TAG, "Hiding loading dialog")
        if (!isFinishing && loadingDialog?.isShowing == true) {
            loadingDialog?.dismiss()
        }
    }

    private fun performLogin(email: String, password: String) {
        Log.d(TAG, "Performing login for: $email")
        val isNetworkAvailable = networkViewModel.networkLiveData.value ?: false

        // LoginViewModel should handle saving credentials to SharedPrefs if needed for offline mode,
        // and MUST use TokenManager to save the auth token.
        // sharedPrefs.saveCredentials(email, password) // Example if you still want LoginActivity to do this

        if (isNetworkAvailable) {
            Log.d(TAG, "Attempting online login")
            viewModel.loginUser(email, password)
        } else {
            Log.d(TAG, "Attempting offline login")
            viewModel.checkOfflineLogin(email, password)
        }
    }

    private fun handleSuccessfulLogin() {
        Log.d(TAG, "Login successful (ViewModel reported success).")
        hideLoading()
        binding.loginButton.isEnabled = true

        navigateToMainActivity()

        // Perform sync using the application-scoped syncManager.
        // The token used by AuthInterceptor will be the one LoginViewModel (via TokenManager)
        // saved to SharedPrefs.
        (application as FarmDataPodApplication).applicationScope.launch {
            try {
                Log.d(TAG, "Attempting sync after successful login.")
                val syncResult = syncManager.performFullSync() // Or syncManager.syncNow()
                if (!syncResult.isSuccessful()) {
                    Log.w(TAG, "Sync issues after login: ${syncResult.error ?: "Details in entity results"}")
                    syncResult.results.forEach { entityResult ->
                        if (entityResult.error != null || !entityResult.successful) {
                            Log.w(TAG, "Sync for ${entityResult.entityName} failed: ${entityResult.error}")
                        }
                    }
                } else {
                    Log.d(TAG, "Sync after login completed successfully.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background sync/data fetch failed after login", e)
            }
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        if (!isFinishing) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LoginActivity being destroyed")
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}