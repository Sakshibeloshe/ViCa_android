package com.vica.app.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vica.app.data.local.CardRepository
import com.vica.app.data.model.CardModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class InboxViewModel(private val repository: CardRepository) : ViewModel() {

    val receivedCards = repository.getReceivedCards()
        .map { list -> list.map { CardModel.from(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
