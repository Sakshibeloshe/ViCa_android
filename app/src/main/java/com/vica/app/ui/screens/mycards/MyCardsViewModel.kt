package com.vica.app.ui.screens.mycards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vica.app.data.local.CardRepository
import com.vica.app.data.model.CardModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyCardsViewModel(private val repository: CardRepository) : ViewModel() {

    val myCards = repository.getMyCards()
        .map { list -> list.map { CardModel.from(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteCard(card: CardModel) {
        viewModelScope.launch {
            repository.deleteCardById(card.id)
        }
    }
}
