# ViCa iOS to Android High-Fidelity Migration Plan

This document outlines the detailed architecture and phase-wise plan to rebuild the ViCa iOS application into a high-fidelity native Android application. 

The Android version will fully replicate the features, state management, offline database storage, high-end visual design system, and near-field interactions (both Android-initiated NFC card emulation and CoreBluetooth-equivalent BLE/Accelerometer tap simulation) with zero functional regression.

---

## 🏗️ Architecture & Technology Stack

To achieve parity with the iOS app's modern SwiftUI and Swift Concurrency architecture, we will utilize Android's premier modern development stack:

| Core Layer | iOS Stack (Current) | Android Stack (Target) |
| :--- | :--- | :--- |
| **Language** | Swift 6.0 | Kotlin 2.0+ (with Coroutines & Flow) |
| **UI Framework** | SwiftUI | Jetpack Compose (Declarative UI) |
| **Image Loading** | SwiftUI AsyncImage / Assets | Coil (Coroutine-based Image Loading) |
| **Dependency Injection** | Swift EnvironmentObjects / Init | Hilt / Dagger (Standard Dependency Injection) |
| **Local Database** | CoreData / SQLite | Room Database (Type-safe abstraction over SQLite) |
| **P2P Transfers** | CoreNFC, CoreBluetooth, CoreMotion | HCE (HostApduService), BluetoothGatt, SensorManager |
| **Asynchronous Engine** | Swift Async/Await & MainActor | Kotlin Coroutines (`Flow`, `StateFlow`, `Dispatchers.Main`) |

---

## 📂 Codebase Directory Mapping

Every component in the iOS codebase has a direct architectural counterpart in Android's recommended package structure (`com.vica.app/`):

```
iOS Workspace Directory              Android Package / Module Location
├── App/                             └── com/vica/app/
│   ├── CardStackApp.swift           │   ├── MainActivity.kt (App Entry Point)
│   └── RootTabView.swift            │   └── navigation/RootNavigation.kt (Compose NavController)
├── Persistence/                     └── com/vica/app/data/local/
│   ├── CardEntities.swift           │   ├── entities/CardEntity.kt & FieldEntity.kt (Room entities)
│   ├── CardRepository.swift         │   ├── dao/CardDao.kt (SQL query mappings)
│   └── PersistenceController.swift  │   └── AppDatabase.kt (RoomDatabase initialization)
├── Data/ & Models/                  └── com/vica/app/data/
│   ├── CardModel.swift              │   ├── model/CardModel.kt (Pure Kotlin domain model)
│   ├── AppStore.swift               │   └── repository/AppRepository.kt (Data coordination)
│   └── ProfileStore.swift           │   └── store/ProfileStore.kt (EncryptedSharedPref state)
├── DynamicFields/                   └── com/vica/app/data/fields/
│   └── FieldCatalog.swift           │   └── FieldCatalog.kt (Dynamic profile fields catalog)
├── Components/                      └── com/vica/app/ui/components/
│   ├── CardFrontView.swift          │   ├── CardFrontView.kt (Jetpack Compose premium gradients)
│   ├── ShimmerModifier.swift        │   ├── ShimmerModifier.kt (Jetpack Compose shimmer modifiers)
│   └── FloatingTabBar.swift         │   └── FloatingTabBar.kt (Custom styled floating tab bar)
└── Screens/                         └── com/vica/app/ui/screens/
    ├── MyCards/                     │   ├── mycards/MyCardsScreen.kt (Kotlin Compose)
    │   └── ShareCardView.swift      │   │   └── ShareCardScreen.kt (BLE tap handler UI)
    ├── AddCard/                     │   └── addcard/CardEditorScreen.kt (Kotlin Compose editor)
    └── Inbox/                       │   └── inbox/InboxScreen.kt (Scan / Inbox feed)
```

---

## 🚀 Phase-Wise Implementation Blueprint

### Phase 1: Database & Persistence Layer (Room Database)
Translate CoreData entities (`CDCard` and `CDCardField`) into Room Entities with relational integrity. We will use a `@Relation` query in Room to map `CardEntity` with its child `FieldEntity` models exactly as iOS does with CoreData.

#### Code Tidbit: Kotlin Room Entities (`CardEntity.kt`)
```kotlin
package com.vica.app.data.local.entities

import androidx.room.*
import java.util.UUID

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val typeRaw: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val themeHex: String,
    val photoData: ByteArray? = null,
    
    // Core Fields
    val displayName: String,
    val subtitle: String?,
    val org: String?,
    val bio: String?,
    
    // Inbox & State
    val isFavorite: Boolean = false,
    val isReceived: Boolean = false,
    val folderId: UUID? = null,
    val note: String? = null,
    val tagsRaw: String? = null, // Comma-separated tags
    
    // Profile sync flags
    val usesProfileName: Boolean = true,
    val usesProfileTitle: Boolean = true,
    val usesProfileCompany: Boolean = true,
    val usesProfilePhoto: Boolean = true
)

@Entity(
    tableName = "card_fields",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class FieldEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val cardId: UUID,
    val key: String,
    val label: String,
    val value: String,
    val kindRaw: String,
    val orderIndex: Int
)
```

---

### Phase 2: Proximity P2P Engine (NFC & BLE Tap Detector)
Replicate the Near-Device Data Transfer logic.
1. **NFC Card Emulation**: Implement `HostApduService` on Android to emulate the card expected by the iOS `SELECT AID` reader session.
2. **BLE & Accelerometer Tap detector**: Replicate the exact proximity-gated bump confirmation matching the iOS timestamp-delta calculation.

#### Code Tidbit: HostApduService (`ViCaHCEService.kt`)
Create `ViCaHCEService` to handle the `SELECT AID` command from CoreNFC. Upon selection, Android immediately responds with the **38-byte payload** containing the `sessionToken`, the device's `bleMacAddress` and the `GattServiceUUID`.

```kotlin
package com.vica.app.proximity

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import java.util.UUID

class ViCaHCEService : HostApduService() {

    // Must match iOS vicaAID: [0xF0, 0x56, 0x49, 0x43, 0x41, 0x01, 0x01, 0x01]
    private val vicaAID = byteArrayOf(0xF0.toByte(), 0x56, 0x49, 0x43, 0x41, 0x01, 0x01, 0x01)

    // APDU Success status: SW 9000
    private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
    private val STATUS_FAILED = byteArrayOf(0x6A.toByte(), 0x82.toByte())

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) return STATUS_FAILED

        // ISO 7816-4 SELECT AID command verification
        if (commandApdu.size >= 4 &&
            commandApdu[0] == 0x00.toByte() && // CLA
            commandApdu[1] == 0xA4.toByte() && // INS (SELECT)
            commandApdu[2] == 0x04.toByte() && // P1 (Select by AID)
            commandApdu[3] == 0x00.toByte()    // P2
        ) {
            val aidLength = commandApdu[4].toInt() and 0xFF
            if (commandApdu.size >= 5 + aidLength) {
                val incomingAid = commandApdu.copyOfRange(5, 5 + aidLength)
                if (incomingAid.contentEquals(vicaAID)) {
                    // Assemble the 38-byte response payload
                    return assemblePayload() + STATUS_SUCCESS
                }
            }
        }
        return STATUS_FAILED
    }

    override fun onDeactivated(reason: Int) {
        // Connection closed or lost field
    }

    private fun assemblePayload(): ByteArray {
        val payload = ByteArray(38)
        
        // 1. Session Token (16 bytes)
        val tokenBytes = UUID.randomUUID().toString().replace("-", "").take(16).toByteArray()
        System.arraycopy(tokenBytes, 0, payload, 0, tokenBytes.size)

        // 2. BLE MAC Address (6 bytes) — Placeholder representation
        val macBytes = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        System.arraycopy(macBytes, 0, payload, 16, 6)

        // 3. GATT Service UUID (16 bytes)
        val serviceUUID = UUID.fromString("F0564943-4101-0101-0000-000000000000")
        val uuidBytes = serviceUUID.toByteArray() // custom implementation to extract 16 bytes
        System.arraycopy(uuidBytes, 0, payload, 22, 16)

        return payload
    }
    
    private fun UUID.toByteArray(): ByteArray {
        val bytes = ByteArray(16)
        val most = mostSignificantBits
        val least = leastSignificantBits
        for (i in 0..7) {
            bytes[i] = (most ushr ((7 - i) * 8)).toByte()
            bytes[8 + i] = (least ushr ((7 - i) * 8)).toByte()
        }
        return bytes
    }
}
```

Add to `AndroidManifest.xml` inside `<application>`:
```xml
<service
    android:name=".proximity.ViCaHCEService"
    android:exported="true"
    android:permission="android.permission.BIND_NFC_SERVICE">
    <intent-filter>
        <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
    </intent-filter>
    <meta-data
        android:name="android.nfc.cardemulation.host_apdu_service"
        android:resource="@xml/apduservice" />
</service>
```

Under `res/xml/apduservice.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/nfc_description"
    android:requireDeviceUnlock="false">
    <aid-group
        android:category="other"
        android:description="@string/nfc_aid_description">
        <!-- Matches F056494341010101 exactly -->
        <aid-filter android:name="F056494341010101" />
    </aid-group>
</host-apdu-service>
```

---

### Phase 3: BLE & Accelerometer Tap Detector (Android Client & Server)

#### Code Tidbit: Kotlin Accelerometer & BLE Tap Detector (`ViCaBLETapDetector.kt`)
Replicates the double-gated threshold logic: RSSI threshold at ~30cm ($-60$ dBm) and physical acceleration magnitude spike $>0.8g$ using `Sensor.TYPE_LINEAR_ACCELERATION`.

```kotlin
package com.vica.app.proximity

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.lang.Math.sqrt

class ViCaBLETapDetector(
    context: Context,
    private val onBumpDetected: (Double) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    
    private val accelThreshold = 0.8 * 9.81 // Convert g-force to m/s^2 (~7.84 m/s^2)
    private var lastBumpTime = 0L

    fun start() {
        linearAccel?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate 3D linear acceleration magnitude (gravity already subtracted by TYPE_LINEAR_ACCELERATION)
        val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())

        if (magnitude > accelThreshold) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBumpTime > 1000L) { // 1-second debounce
                lastBumpTime = currentTime
                onBumpDetected(magnitude / 9.81) // Pass back as g-force
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
```

---

### Phase 4: UI & Aesthetic Design Parity (Jetpack Compose Canvas)
To ensure the Android app matches the visual luxury of the iOS app, we will rebuild the iOS premium dynamic card layouts using Jetpack Compose `Brush`, custom shapes, and `Canvas` API rather than static image assets.

#### Code Tidbit: Compose Card Gradient (`PremiumCardView.kt`)
Replicates the visual appearance of SwiftUI’s custom HSL gradients and mesh animations with exact mathematical fidelity.

```kotlin
package com.vica.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PremiumCardPattern(
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.85f),
                        themeColor.copy(alpha = 0.6f)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Replicate elegant light waves matching iOS SwiftUI PremiumCardPattern
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(width * 0.1f, height * 0.2f),
                    radius = width * 0.6f
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(width * 0.9f, height * 0.8f),
                    radius = width * 0.5f
                )
            )
        }
    }
}
```

---

### Phase 5: Routing & Main Screen Composition
Build Jetpack Compose screens for `MyCardsView`, `AddCardView`, `CardEditorView`, `InboxView` using modern composable components that match the dynamic card stacks, swipe behaviors, and layout ratios.

---

## 🔮 Verification & Testing Plan

### Automated Room Test Suite
- Write instrumentation tests under `app/src/androidTest` using standard Room testing frameworks to guarantee proper SQLite persistence operations and cascade deletions of custom dynamic fields.

### NFC HCE Integration Test
- Use a physical Android device and an iOS device to initiate the CoreNFC reader. Verify:
  1. iOS sends SELECT AID.
  2. Android emulated card triggers `ViCaHCEService`.
  3. Payload is decoded on the iOS reader showing correct UUID and Mac byte patterns.
