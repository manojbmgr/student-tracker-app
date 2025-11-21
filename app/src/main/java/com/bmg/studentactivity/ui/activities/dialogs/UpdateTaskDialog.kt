package com.bmg.studentactivity.ui.activities.dialogs

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bmg.studentactivity.data.models.Activity
import com.bmg.studentactivity.databinding.DialogUpdateTaskBinding
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class UpdateTaskDialog : DialogFragment() {
    private var _binding: DialogUpdateTaskBinding? = null
    private val binding get() = _binding!!
    
    private var activity: Activity? = null
    private var studentEmail: String? = null
    private var onUpdateComplete: ((Activity) -> Unit)? = null
    private var selectedImageUri: Uri? = null
    private var photoFile: File? = null
    
    @Inject
    lateinit var activityRepository: com.bmg.studentactivity.data.repository.ActivityRepository
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            Glide.with(requireContext())
                .load(selectedImageUri)
                .into(binding.imgSelectedImage)
            binding.imgSelectedImage.visibility = View.VISIBLE
            binding.btnRemoveImage.visibility = View.VISIBLE
        }
    }
    
    private fun launchCamera() {
        try {
            photoFile = createImageFile()
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile!!
            )
            selectedImageUri = photoURI
            cameraLauncher.launch(photoURI)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    companion object {
        private const val ARG_ACTIVITY = "arg_activity"
        private const val ARG_STUDENT_EMAIL = "arg_student_email"
        
        fun newInstance(
            activity: Activity,
            studentEmail: String,
            onUpdateComplete: (Activity) -> Unit
        ): UpdateTaskDialog {
            val dialog = UpdateTaskDialog()
            dialog.onUpdateComplete = onUpdateComplete
            val args = Bundle().apply {
                putString(ARG_ACTIVITY, com.google.gson.Gson().toJson(activity))
                putString(ARG_STUDENT_EMAIL, studentEmail)
            }
            dialog.arguments = args
            return dialog
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_MaterialComponents_Dialog)
        
        try {
            arguments?.let {
                val activityJson = it.getString(ARG_ACTIVITY)
                if (activityJson != null) {
                    activity = com.google.gson.Gson().fromJson(activityJson, Activity::class.java)
                }
                studentEmail = it.getString(ARG_STUDENT_EMAIL)
            }
            
            if (activity == null || studentEmail.isNullOrEmpty()) {
                android.util.Log.e("UpdateTaskDialog", "Activity or studentEmail is null")
                dismiss()
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateTaskDialog", "Error in onCreate: ${e.message}", e)
            dismiss()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUpdateTaskBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupListeners()
    }
    
    private fun setupUI() {
        try {
            val currentActivity = activity
            val email = studentEmail
            
            if (currentActivity == null || email.isNullOrEmpty()) {
                android.util.Log.e("UpdateTaskDialog", "Cannot setup UI: activity or email is null")
                dismiss()
                return
            }
            
            binding.tvTaskTitle.text = currentActivity.displayTitle
            
            // Set current status
            val isCurrentlyCompleted = currentActivity.isCompleted == true || currentActivity.isCompletedToday == true
            binding.switchComplete.isChecked = isCurrentlyCompleted
            
            // Show current remark if exists
            if (currentActivity.remark != null && currentActivity.remark.isNotEmpty()) {
                binding.etRemark.setText(currentActivity.remark)
            }
            
            // Show current completion image if exists
            if (currentActivity.hasCompletionImage && currentActivity.completionImageUrl != null) {
                Glide.with(requireContext())
                    .load(currentActivity.completionImageUrl)
                    .into(binding.imgSelectedImage)
                binding.imgSelectedImage.visibility = View.VISIBLE
                binding.btnRemoveImage.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateTaskDialog", "Error in setupUI: ${e.message}", e)
            Toast.makeText(requireContext(), "Error loading dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupListeners() {
        binding.btnCaptureImage.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
        
        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            photoFile = null
            binding.imgSelectedImage.visibility = View.GONE
            binding.btnRemoveImage.visibility = View.GONE
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveTask()
        }
    }
    
    private fun checkCameraPermissionAndLaunch() {
        try {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    launchCamera()
                }
                else -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateTaskDialog", "Error checking camera permission: ${e.message}", e)
            Toast.makeText(requireContext(), "Error accessing camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveTask() {
        val currentActivity = activity ?: return
        val email = studentEmail ?: return
        val isCompleted = binding.switchComplete.isChecked
        val remark = binding.etRemark.text.toString().takeIf { it.isNotEmpty() }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = if (selectedImageUri != null) {
                    activityRepository.completeActivityWithImage(
                        studentEmail = email,
                        timetableId = currentActivity.timetableId,
                        activityId = currentActivity.activityId,
                        isCompleted = isCompleted,
                        remark = remark,
                        imageUri = selectedImageUri
                    )
                } else {
                    activityRepository.completeActivity(
                        studentEmail = email,
                        timetableId = currentActivity.timetableId,
                        activityId = currentActivity.activityId,
                        isCompleted = isCompleted,
                        remark = remark
                    )
                }
                
                result.onSuccess { response ->
                    if (response.success) {
                        Toast.makeText(requireContext(), response.message ?: "Task updated successfully", Toast.LENGTH_SHORT).show()
                        onUpdateComplete?.invoke(currentActivity)
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), response.message ?: "Failed to update task", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { exception ->
                    Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

