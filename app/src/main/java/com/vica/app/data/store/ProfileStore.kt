package com.vica.app.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ProfileStore — stores the user's own identity (name, title, company, photo path).
 * Uses SharedPreferences. Backed by StateFlow for reactive UI updates.
 * In production, swap SharedPreferences for EncryptedSharedPreferences.
 */
class ProfileStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vica_profile", Context.MODE_PRIVATE)

    private val _name    = MutableStateFlow(prefs.getString(KEY_NAME, "") ?: "")
    private val _title   = MutableStateFlow(prefs.getString(KEY_TITLE, "") ?: "")
    private val _company = MutableStateFlow(prefs.getString(KEY_COMPANY, "") ?: "")

    val name:    StateFlow<String> = _name.asStateFlow()
    val title:   StateFlow<String> = _title.asStateFlow()
    val company: StateFlow<String> = _company.asStateFlow()

    fun setName(value: String) {
        prefs.edit { putString(KEY_NAME, value) }
        _name.value = value
    }

    fun setTitle(value: String) {
        prefs.edit { putString(KEY_TITLE, value) }
        _title.value = value
    }

    fun setCompany(value: String) {
        prefs.edit { putString(KEY_COMPANY, value) }
        _company.value = value
    }

    companion object {
        private const val KEY_NAME    = "profile_name"
        private const val KEY_TITLE   = "profile_title"
        private const val KEY_COMPANY = "profile_company"
    }
}
