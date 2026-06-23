package com.vica.app.ui.screens.addcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vica.app.data.fields.FieldCatalog
import com.vica.app.data.fields.FieldDefinition
import com.vica.app.data.local.CardRepository
import com.vica.app.data.model.CardTheme
import com.vica.app.data.model.CardType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CardEditorViewModel(private val repository: CardRepository) : ViewModel() {

    private val _selectedType = MutableStateFlow(CardType.PERSONAL)
    val selectedType: StateFlow<CardType> = _selectedType.asStateFlow()

    private val _selectedTheme = MutableStateFlow(CardTheme.PINK)
    val selectedTheme: StateFlow<CardTheme> = _selectedTheme.asStateFlow()

    private val _fieldValues = MutableStateFlow<Map<String, String>>(emptyMap())
    val fieldValues: StateFlow<Map<String, String>> = _fieldValues.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val fieldDefinitions: List<FieldDefinition>
        get() = FieldCatalog.fields(_selectedType.value)

    fun setType(type: CardType) {
        _selectedType.value = type
        _fieldValues.value = emptyMap()  // Reset fields when type changes
    }

    fun setTheme(theme: CardTheme) {
        _selectedTheme.value = theme
    }

    fun updateField(key: String, value: String) {
        _fieldValues.value = _fieldValues.value.toMutableMap().apply { put(key, value) }
    }

    fun saveCard(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.createCard(
                    type   = _selectedType.value,
                    values = _fieldValues.value,
                    photoBytes = null,
                    theme  = _selectedTheme.value
                )
                onSuccess()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
