package com.vica.app.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vica.app.data.local.AppDatabase
import com.vica.app.data.local.CardRepository
import com.vica.app.proximity.ProximityManager
import com.vica.app.proximity.ViCaNFCPayload
import com.vica.app.ui.components.FloatingTabBar
import com.vica.app.ui.components.ViCaTab
import com.vica.app.ui.screens.addcard.CardEditorScreen
import com.vica.app.ui.screens.addcard.CardEditorViewModel
import com.vica.app.ui.screens.inbox.InboxScreen
import com.vica.app.ui.screens.inbox.InboxViewModel
import com.vica.app.ui.screens.mycards.MyCardsScreen
import com.vica.app.ui.screens.mycards.MyCardsViewModel
import com.vica.app.ui.screens.mycards.NfcTransferViewModel
import com.vica.app.ui.screens.mycards.ReceivedCardScreen
import com.vica.app.ui.screens.mycards.ShareCardScreen
import com.vica.app.ui.screens.scan.ScanScreen
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun RootNavigation(
    nfcEvents: SharedFlow<Pair<ViCaNFCPayload, ByteArray?>>
) {
    val context = LocalContext.current

    val db               = remember { AppDatabase.getDatabase(context) }
    val repository       = remember { CardRepository(db.cardDao()) }
    val proximityManager = remember { ProximityManager(context) }

    val myCardsVm: MyCardsViewModel  = viewModel(factory = MyCardsViewModelFactory(repository))
    val inboxVm: InboxViewModel       = viewModel(factory = InboxViewModelFactory(repository))
    val editorVm: CardEditorViewModel = viewModel(factory = CardEditorViewModelFactory(repository))
    val transferVm: NfcTransferViewModel = viewModel(
        factory = NfcTransferViewModelFactory(repository, proximityManager)
    )

    val myCards        by myCardsVm.myCards.collectAsStateWithLifecycle()
    val received       by inboxVm.receivedCards.collectAsStateWithLifecycle()
    val proximityState by proximityManager.state.collectAsStateWithLifecycle()
    val transferState  by transferVm.transferState.collectAsStateWithLifecycle()

    var selectedTab    by remember { mutableStateOf(ViCaTab.MY_CARDS) }
    var shareCardIndex by remember { mutableStateOf<Int?>(null) }

    // ── Collect NFC tap events from MainActivity ────────────────────────────
    // This device is acting as the RECEIVER when an event arrives here.
    // Forward the payload to the TransferViewModel for decoding and persistence.
    LaunchedEffect(nfcEvents) {
        nfcEvents.collect { (payload, cardBytes) ->
            transferVm.onNfcPayloadReceived(payload, cardBytes)
        }
    }

    DisposableEffect(Unit) {
        onDispose { proximityManager.destroy() }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        val shareIdx = shareCardIndex

        when {

            // ── Receiver overlay — NFC card received ────────────────────────
            transferState is NfcTransferViewModel.TransferState.Received -> {
                val state = transferState as NfcTransferViewModel.TransferState.Received
                ReceivedCardScreen(
                    card      = state.card,
                    onSave    = {
                        // Card already saved in ViewModel; just dismiss and go to Inbox
                        transferVm.dismissReceived()
                        selectedTab = ViCaTab.INBOX
                    },
                    onDismiss = { transferVm.dismissReceived() }
                )
            }

            // ── Sender overlay — Share sheet open ──────────────────────────
            shareIdx != null && shareIdx < myCards.size -> {
                ShareCardScreen(
                    card           = myCards[shareIdx],
                    proximityState = proximityState,
                    onDismiss      = {
                        transferVm.cancelSend()
                        shareCardIndex = null
                    }
                )
            }

            // ── Main tab navigation ────────────────────────────────────────
            else -> {
                when (selectedTab) {
                    ViCaTab.MY_CARDS -> MyCardsScreen(
                        cards       = myCards,
                        onCardClick = { card ->
                            val idx = myCards.indexOf(card)
                            if (idx >= 0) {
                                shareCardIndex = idx
                                transferVm.prepareSend(card)   // Arms HCE + BLE
                            }
                        },
                        onShareCard = { card ->
                            val idx = myCards.indexOf(card)
                            if (idx >= 0) {
                                shareCardIndex = idx
                                transferVm.prepareSend(card)
                            }
                        },
                        onAddCard = { selectedTab = ViCaTab.ADD_CARD }
                    )
                    ViCaTab.ADD_CARD -> {
                        val type     by editorVm.selectedType.collectAsStateWithLifecycle()
                        val theme    by editorVm.selectedTheme.collectAsStateWithLifecycle()
                        val values   by editorVm.fieldValues.collectAsStateWithLifecycle()
                        val isSaving by editorVm.isSaving.collectAsStateWithLifecycle()
                        CardEditorScreen(
                            selectedType     = type,
                            selectedTheme    = theme,
                            fieldValues      = values,
                            fieldDefinitions = editorVm.fieldDefinitions,
                            isSaving         = isSaving,
                            onTypeSelected   = editorVm::setType,
                            onThemeSelected  = editorVm::setTheme,
                            onFieldChanged   = editorVm::updateField,
                            onSave           = { editorVm.saveCard { selectedTab = ViCaTab.MY_CARDS } },
                            onBack           = { selectedTab = ViCaTab.MY_CARDS }
                        )
                    }
                    ViCaTab.INBOX -> InboxScreen(cards = received)
                    ViCaTab.SCAN  -> ScanScreen()
                }

                FloatingTabBar(
                    selectedTab   = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier      = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ─── ViewModel Factories ──────────────────────────────────────────────────────

class MyCardsViewModelFactory(private val repo: CardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") return MyCardsViewModel(repo) as T
    }
}

class InboxViewModelFactory(private val repo: CardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") return InboxViewModel(repo) as T
    }
}

class CardEditorViewModelFactory(private val repo: CardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST") return CardEditorViewModel(repo) as T
    }
}

class NfcTransferViewModelFactory(
    private val repo: CardRepository,
    private val proximityManager: ProximityManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NfcTransferViewModel(repo, proximityManager) as T
    }
}
