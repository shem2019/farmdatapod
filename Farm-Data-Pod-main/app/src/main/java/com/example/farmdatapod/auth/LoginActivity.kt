package com.example.farmdatapod.auth


import android.app.Application
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
import com.example.farmdatapod.databinding.ActivityLoginBinding
import com.example.farmdatapod.utils.SharedPrefs
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.data.DataSyncManager
import com.example.farmdatapod.network.NetworkViewModel
import com.example.farmdatapod.utils.DialogUtils
import com.example.farmdatapod.models.LoginState
import com.example.farmdatapod.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private lateinit var dbHandler: DBHandler
    private lateinit var sharedPrefs: SharedPrefs
    private lateinit var networkViewModel: NetworkViewModel
    private lateinit var dataSyncManager: DataSyncManager
    private lateinit var syncManager: SyncManager
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
            viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
            networkViewModel = ViewModelProvider(this)[NetworkViewModel::class.java]
            dbHandler = DBHandler(this)
            sharedPrefs = SharedPrefs(this)

            val apiService = RestClient.getApiService(this)
            dataSyncManager = DataSyncManager(apiService, sharedPrefs, dbHandler, this)

            // Get SyncManager from Application
            syncManager = (application as FarmDataPodApplication).getSyncManager()
            Log.d(TAG, "Got SyncManager from Application")

            dialogUtils = DialogUtils(this)
            loadingDialog = dialogUtils.buildLoadingDialog()

            checkDatabaseStatus()
            Log.d(TAG, "Components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
            showError("Failed to initialize app components")
        }
    }

    private fun checkDatabaseStatus() {
        try {
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
        val email = binding.username.text.toString()
        val password = binding.password.text.toString()

        if (validateFields(email, password)) {
            performLogin(email, password)
        }
    }




    private fun handleLoginError(message: String) {
        Log.e(TAG, "Login error: $message")
        hideLoading()
        showError(message)
    }

    private fun handleOfflineState() {
        Log.d(TAG, "Handling offline state")
        hideLoading()
        binding.loginButton.isEnabled = true
        showError("You are offline. Using cached credentials.")
    }

    private fun checkAutoLogin() {
        if (viewModel.isUserLoggedIn()) {
            Log.d(TAG, "Auto login: User already logged in")
            navigateToMainActivity()
        }
    }

    private fun updateNetworkStatus(isAvailable: Boolean) {
        Log.d(TAG, "Updating network status: $isAvailable")
        binding.networkStatusText?.apply {
            text = if (isAvailable) "Online" else "Offline"
            setTextColor(getColor(
                if (isAvailable) android.R.color.holo_green_dark
                else android.R.color.holo_red_dark
            ))
        }
    }

    private fun validateFields(email: String, password: String): Boolean {
        var isValid = true
        Log.d(TAG, "Validating login fields")

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

    private fun saveCredentials(email: String, password: String) {
        Log.d(TAG, "Saving credentials for: $email")
        sharedPrefs.saveCredentials(email, password)
    }

    private fun showLoading(message: String = "Loading...") {
        Log.d(TAG, "Showing loading dialog: $message")
        if (!isFinishing) {
            loadingDialog?.setMessage(message)
            loadingDialog?.show()
        }
    }

    private fun hideLoading() {
        Log.d(TAG, "Hiding loading dialog")
        if (!isFinishing) {
            loadingDialog?.dismiss()
        }
    }

    private fun performLogin(email: String, password: String) {
        Log.d(TAG, "Performing login for: $email")
        val isNetworkAvailable = networkViewModel.networkLiveData.value ?: false

        // Save credentials before attempting login
        sharedPrefs.saveCredentials(email, password)

        if (isNetworkAvailable) {
            Log.d(TAG, "Attempting online login")
            viewModel.loginUser(email, password)
        } else {
            Log.d(TAG, "Attempting offline login")
            viewModel.checkOfflineLogin(email, password)
        }
    }

    private fun handleSuccessfulLogin() {
        Log.d(TAG, "Login successful")

        // Navigate immediately
        navigateToMainActivity()

        // Run background sync from application scope
        (application as FarmDataPodApplication).applicationScope.launch {
            try {
                dataSyncManager.fetchAndSyncAllData()
                val syncResult = syncManager.syncNow()
                if (!syncResult.isSuccessful()) {
                    Log.w(TAG, "Sync issues: ${syncResult.error}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background sync failed", e)
            }
        }
    }



    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        //finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LoginActivity being destroyed")
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}