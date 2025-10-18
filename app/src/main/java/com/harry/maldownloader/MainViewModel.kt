package com.harry.maldownloader

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val _notificationPermissionGranted = MutableLiveData(false)
    val notificationPermissionGranted: LiveData<Boolean> = _notificationPermissionGranted

    private val _storagePermissionGranted = MutableLiveData(false)
    val storagePermissionGranted: LiveData<Boolean> = _storagePermissionGranted

    fun setNotificationPermission(granted: Boolean) {
        _notificationPermissionGranted.postValue(granted)
    }

    fun setStoragePermission(granted: Boolean) {
        _storagePermissionGranted.postValue(granted)
    }

    fun checkInitialPermissions() {
        val context = getApplication<Application>()
        val notificationGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        _notificationPermissionGranted.postValue(notificationGranted)

        val storageGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        _storagePermissionGranted.postValue(storageGranted)
    }

    // ... your existing functions
}
