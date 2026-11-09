package com.rasd.app.ui.screens.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasd.app.data.dao.AppSettingsDao
import com.rasd.app.data.model.AppSettingsEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: AppSettingsDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AppSettingsEntity())
    val state: StateFlow<AppSettingsEntity> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDao.getSettingsOnce()?.let { _state.value = it }
        }
    }

    fun update(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        _state.value = transform(_state.value)
    }

    fun save() {
        viewModelScope.launch {
            settingsDao.insertSettings(_state.value.copy(id = 1))
            _message.value = appContext.getString(com.rasd.app.R.string.msg_saved_settings)
        }
    }

    /** يبدّل لغة التطبيق (أندرويد 13+ يطبّقها فوراً بدون إعادة تشغيل). */
    fun setLanguage(code: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(language = code)
            settingsDao.insertSettings(_state.value.copy(id = 1))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    val lm = appContext.getSystemService(android.app.LocaleManager::class.java)
                    lm?.applicationLocales = android.os.LocaleList.forLanguageTags(code)
                }
            }
            _message.value = appContext.getString(com.rasd.app.R.string.msg_saved_settings)
        }
    }

    fun importLogo(uri: Uri) {
        viewModelScope.launch {
            val dir = File(appContext.filesDir, "logo").apply { mkdirs() }
            val out = File(dir, "report_logo.png")
            val decoded = runCatching {
                appContext.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
            }.getOrNull()
            if (decoded == null) {
                _message.value = appContext.getString(com.rasd.app.R.string.msg_cannot_read_image)
                return@launch
            }
            val scaled = if (decoded.width > 512 || decoded.height > 512) {
                val ratio = min(512f / decoded.width, 512f / decoded.height)
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }
            FileOutputStream(out).use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
            _state.value = _state.value.copy(logoPath = out.absolutePath)
            settingsDao.insertSettings(_state.value.copy(id = 1))
            _message.value = appContext.getString(com.rasd.app.R.string.msg_logo_updated)
        }
    }

    fun clearLogo() {
        viewModelScope.launch {
            _state.value = _state.value.copy(logoPath = "")
            settingsDao.insertSettings(_state.value.copy(id = 1))
            _message.value = appContext.getString(com.rasd.app.R.string.msg_logo_removed)
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
