package com.example.cbtsmpdoabangsa.presentation.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.cbtsmpdoabangsa.R
import com.example.cbtsmpdoabangsa.databinding.ActivityHomeBinding
import com.example.cbtsmpdoabangsa.presentation.camera.CameraActivity
import com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeViewModel by viewModels()

    companion object {
        const val LINK_EXTRA = "link"
    }

    private val cameraPermission = Manifest.permission.CAMERA
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            handlePermission(granted = permissionGranted)
            if (permissionGranted) {
                barcodeLauncher.launch(Intent(this, CameraActivity::class.java))
            }
        }

    private val barcodeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                binding.etlLink.editText?.setText(result.data?.getStringExtra(LINK_EXTRA).toString())
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        handleState()

        binding.btnQr.setOnClickListener {
            cameraPermissionLauncher.launch(cameraPermission)
        }

        binding.btnStart.setOnClickListener {
            viewModel.updateUsername(
                username = binding.etlName.editText?.text.toString(),
                userClassType = binding.etlClass.editText?.text.toString(),
                examType = binding.checkbox.isChecked
            )
        }
    }

    private fun handleState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.username.collectLatest {
                        binding.etlName.editText?.setText(it)
                    }
                }
                launch {
                    viewModel.userClass.collect {
                        binding.actvClass.setText(it, false)
                    }
                }
                launch {
                    viewModel.examType.collectLatest {
                        binding.checkbox.isChecked = it
                    }
                }
            }
        }
    }

    private fun handlePermission(granted: Boolean) {
        val appDetailIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", this.packageName, null)
        )

        if (!granted) {
            MaterialAlertDialogBuilder(this, ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_camera))
                .setTitle("Butuh Izin Akses Camera")
                .setMessage(
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, cameraPermission)) {
                        "Fitur ini membutuhkan akses kamera"
                    } else {
                        "Izin akses kamera sudah di tolak permanen, buka pengaturan untuk mengizinkan"
                    }
                )
                .setPositiveButton(
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, cameraPermission)) {
                        "Izinkan"
                    } else {
                        "Buka pengaturan"
                    }
                ) { dialog, _ ->
                    dialog.dismiss()
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, cameraPermission)) {
                        cameraPermissionLauncher.launch(cameraPermission)
                    } else {
                        startActivity(appDetailIntent)
                    }
                }
                .setNegativeButton("Batalkan") { dialog, _ ->
                    dialog.cancel()
                }.create().show()
        }
    }
}