package com.example.cbtsmpdoabangsa.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.WindowManager.LayoutParams
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.drawToBitmap
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cbtsmpdoabangsa.R
import com.example.cbtsmpdoabangsa.databinding.ActivityMainBinding
import com.example.cbtsmpdoabangsa.presentation.camera.CameraActivity
import com.example.cbtsmpdoabangsa.presentation.main.HomeActivity.Companion.LINK_EXTRA
import com.example.cbtsmpdoabangsa.service.CBTService
import com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminName: ComponentName

    private lateinit var cbtService: CBTService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CBTService.CBTBinder
            cbtService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private val cameraPermission = Manifest.permission.CAMERA
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                launchBarcodeScanner()
            } else {
                handlePermissionNotGranted()
            }
        }

    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                binding.etl.editText?.setText(
                    result.data?.getStringExtra(LINK_EXTRA).toString()
                )
                if (result.data?.getStringExtra(LINK_EXTRA).toString() != "abc36000620b96ffe75a7a2c9494a4c3"
                    && result.data?.getStringExtra(LINK_EXTRA).toString() != "280f3a02564b43f4b2adbaee29cda57a"
                ) {
                    searchBarcode()
                } else {
                    binding.etl.editText?.setText("")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        window.setFlags(LayoutParams.FLAG_SECURE, LayoutParams.FLAG_SECURE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        savedInstanceState?.let {
            binding.webview.restoreState(it)
        }
        binding.webview.setOnLongClickListener {
            true
        }
        binding.webview.isLongClickable = false
        binding.webview.isHapticFeedbackEnabled = false

        initView()
        initWebView()
        initKioskMode()
        initState()
    }

    private fun initView() {
        binding.btnBack.setOnClickListener {
            binding.webview.goBack()
            hideSoftKeyboard()
        }

        binding.btnBarcode.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    cameraPermission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    launchBarcodeScanner()
                }

                shouldShowRequestPermissionRationale(cameraPermission) -> {
                    handlePermissionNotGranted()
                }

                else -> {
                    cameraPermissionLauncher.launch(cameraPermission)
                }
            }
        }

        binding.btnRefresh.setOnClickListener {
            hideSoftKeyboard()
            binding.webview.reload()
        }

        binding.etl.editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search()
                Log.d("TAG", "initView: tigger")
            }
            false
        }

        binding.etl.editText?.doOnTextChanged { text, _, _, _ ->
            var isAdmin = false
            if (dpm.isDeviceOwnerApp(packageName)) {
                //Toast.makeText(applicationContext, "owner", Toast.LENGTH_SHORT).show()
                isAdmin = true
            }
            when {
                text.toString() == "keluaraplikasi" -> {
                    setKioskPolicies(enable = false, isAdmin)
                    finish()
                    finishAndRemoveTask()
                }

                text.toString() == "abc36000620b96ffe75a7a2c9494a4c3" -> {
                    setKioskPolicies(enable = false, isAdmin)
                    finish()
                    finishAndRemoveTask()
                }

                text.toString() == "unpinaplikasi" -> {
                    setKioskPolicies(enable = false, isAdmin)
                }

                text.toString() == "280f3a02564b43f4b2adbaee29cda57a" -> {
                    setKioskPolicies(enable = false, isAdmin)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.proggressBar.visibility = View.VISIBLE
                viewModel.updateLink(link = url.toString())
                viewModel.updateBackActionAvailable(available = binding.webview.canGoBack())
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.proggressBar.visibility = View.GONE
            }
        }
        binding.webview.settings.javaScriptEnabled = true
    }

    private fun initKioskMode() {
        dpm =
            applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminName = componentName
        var isAdmin = false
        if (dpm.isDeviceOwnerApp(packageName)) {
            //Toast.makeText(applicationContext, "owner", Toast.LENGTH_SHORT).show()
            isAdmin = true
        }
        //setKioskPolicies(enable = true, isAdmin)
    }

    private fun initState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.linkState.collect { link ->
                        if (link.trim().isNotBlank()) {
                            binding.etl.editText?.setText(link)
                            //binding.textField.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                        }
                    }
                }

                launch {
                    viewModel.canGoBackState.collect { available ->
                        if (available) {
                            binding.btnBack.visibility = View.VISIBLE
                        } else {
                            binding.btnBack.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun search() {
        hideSoftKeyboard()
        binding.etl.editText?.clearFocus()
        val link = binding.etl.editText?.text.toString()
        if (!link.lowercase().contains("google") &&
            !link.lowercase().contains("bing") &&
            !link.lowercase().contains("yahoo")
        ) {
            viewModel.updateLink(link)
            binding.webview.loadUrl(link)
        } else {
            Snackbar.make(
                binding.root,
                "Tidak boleh membuka link selain link ujian",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun searchBarcode() {
        val link = binding.etl.editText?.text.toString()
        viewModel.updateLink(link)
        binding.webview.loadUrl(link)
    }

    private fun hideSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
    }

    private fun launchBarcodeScanner() {
        barcodeLauncher.launch(Intent(this, CameraActivity::class.java))
    }

    private fun handlePermissionNotGranted() {
        val appDetailIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", this.packageName, null)
        )

        MaterialAlertDialogBuilder(this, ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .setIcon(R.drawable.ic_camera)
            .setTitle("Butuh Izin Akses Camera")
            .setMessage(
                if (shouldShowRequestPermissionRationale(cameraPermission)) {
                    "Fitur ini membutuhkan akses kamera"
                } else {
                    "Izin akses kamera sudah di tolak permanen, buka pengaturan untuk mengizinkan"
                }
            )
            .setPositiveButton(
                if (shouldShowRequestPermissionRationale(cameraPermission)) {
                    "Izinkan"
                } else {
                    "Buka pengaturan"
                }
            ) { dialog, _ ->
                dialog.dismiss()
                if (shouldShowRequestPermissionRationale(cameraPermission)) {
                    cameraPermissionLauncher.launch(cameraPermission)
                } else {
                    startActivity(appDetailIntent)
                }
            }
            .setNegativeButton("Batalkan") { dialog, _ ->
                dialog.cancel()
            }.create().show()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, CBTService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        //setKioskPolicies(true, false)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }

    private fun setKioskPolicies(enable: Boolean, isAdmin: Boolean) {
        if (isAdmin) {
            setRestrictions(enable)
            enableStayOnWhilePluggedIn(enable)
            setUpdatePolicy(enable)
            setAsHomeApp(enable)
            setKeyGuardEnabled(enable)
        }
        setLockTask(enable, isAdmin)
        //setImmersiveMode(enable)
    }

    private fun setRestrictions(disallow: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, disallow)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, disallow)
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, disallow)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) = if (disallow) {
        dpm.addUserRestriction(adminName, restriction)
    } else {
        dpm.clearUserRestriction(adminName, restriction)
    }

    private fun enableStayOnWhilePluggedIn(active: Boolean) = if (active) {
        dpm.setGlobalSetting(
            adminName,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            (BatteryManager.BATTERY_PLUGGED_AC
                    or BatteryManager.BATTERY_PLUGGED_USB
                    or BatteryManager.BATTERY_PLUGGED_WIRELESS).toString()
        )
    } else {
        dpm.setGlobalSetting(adminName, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "0")
    }

    private fun setLockTask(start: Boolean, isAdmin: Boolean) {
        if (isAdmin) {
            dpm.setLockTaskPackages(adminName, if (start) arrayOf(packageName) else arrayOf())
        }
        if (start) {
            startLockTask()
        } else {
            stopLockTask()
        }
    }

    private fun setUpdatePolicy(enable: Boolean) {
        if (enable) {
            dpm.setSystemUpdatePolicy(
                adminName,
                SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
            )
        } else {
            dpm.setSystemUpdatePolicy(adminName, null)
        }
    }

    private fun setAsHomeApp(enable: Boolean) {
        if (enable) {
            val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(
                adminName, intentFilter, ComponentName(packageName, MainActivity::class.java.name)
            )
        } else {
            dpm.clearPackagePersistentPreferredActivities(
                adminName, packageName
            )
        }
    }

    private fun setKeyGuardEnabled(enable: Boolean) {
        dpm.setKeyguardDisabled(adminName, !enable)
    }

    /*private fun setImmersiveMode(enable: Boolean) {
        if (enable) {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.decorView.systemUiVisibility = flags
        } else {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            window.decorView.systemUiVisibility = flags
        }
    }*/

    override fun onSaveInstanceState(outState: Bundle) {
        binding.webview.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        if (isTopResumedActivity) {
            if (isInMultiWindowMode) {
                Log.d("onTopResumedActivityChanged: ", "split")
            } else {
                Log.d("onTopResumedActivityChanged: ", "not split")
            }
        }
    }
}