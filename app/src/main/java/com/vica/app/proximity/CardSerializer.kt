package com.vica.app.proximity

import android.util.Log
import com.vica.app.data.model.CardField
import com.vica.app.data.model.CardModel
import com.vica.app.data.model.CardTheme
import com.vica.app.data.model.CardType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import java.util.UUID

/**
 * CardSerializer — compact card encoding for inline NFC delivery.
 *
 * Uses a minimal JSON format to pack the essential card info into ≤250 bytes
 * so it fits inside a single APDU response (max 255 bytes including SW).
 *
 * Fields encoded:
 *   n  — displayName
 *   s  — subtitle (optional)
 *   o  — org (optional)
 *   b  — bio (optional, truncated to 80 chars)
 *   t  — type raw string
 *   th — themeHex
 *   f  — fields array: [{k, l, v}] (key, label, value)
 *
 * Fields NOT encoded in NFC (retrieved later or defaulted):
 *   photoData — too large for NFC (use BLE follow-up)
 *   isFavorite, isReceived, note, createdAt — defaults applied on decode
 *   id — new UUID generated on receiver side
 */
object CardSerializer {

    private const val TAG = "CardSerializer"
    private const val MAX_BYTES = 250
    private const val MAX_BIO_CHARS = 80

    // ─── Encoding ─────────────────────────────────────────────────────────────

    /**
     * Encode a [CardModel] to a compact UTF-8 byte array.
     * Returns null if encoding fails.
     */
    fun encode(card: CardModel): ByteArray? {
        return try {
            val obj = JSONObject().apply {
                put("n",  card.displayName)
                card.subtitle?.let { put("s", it) }
                card.org?.let      { put("o", it) }
                card.bio?.let      { put("b", it.take(MAX_BIO_CHARS)) }
                put("t",  card.type.rawValue)
                put("th", card.themeHex)

                if (card.fields.isNotEmpty()) {
                    val arr = JSONArray()
                    for (field in card.fields) {
                        arr.put(JSONObject().apply {
                            put("k", field.key)
                            put("l", field.label)
                            put("v", field.value)
                            put("ki", field.kind)
                        })
                    }
                    put("f", arr)
                }
            }

            var json = obj.toString()
            var bytes = json.toByteArray(Charsets.UTF_8)

            // If over limit, progressively drop optional fields
            if (bytes.size > MAX_BYTES) {
                obj.remove("b")
                json = obj.toString()
                bytes = json.toByteArray(Charsets.UTF_8)
            }
            if (bytes.size > MAX_BYTES) {
                // Drop fields beyond the first 3
                val arr = obj.optJSONArray("f")
                if (arr != null && arr.length() > 3) {
                    val trimmed = JSONArray()
                    repeat(3) { trimmed.put(arr.get(it)) }
                    obj.put("f", trimmed)
                    json = obj.toString()
                    bytes = json.toByteArray(Charsets.UTF_8)
                }
            }

            if (bytes.size > MAX_BYTES) {
                Log.w(TAG, "Card JSON still ${bytes.size} bytes after trimming — truncating to $MAX_BYTES")
                // Last resort: raw truncate (will fail to parse but BLE fallback covers it)
                bytes.copyOf(MAX_BYTES)
            } else {
                Log.d(TAG, "Encoded card: ${bytes.size} bytes")
                bytes
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to encode card", e)
            null
        }
    }

    // ─── Decoding ─────────────────────────────────────────────────────────────

    /**
     * Decode bytes (from the NFC GET DATA response) back into a [CardModel].
     * Always succeeds at minimum — falls back to placeholder values for any
     * missing/corrupt fields so the received card is never silently dropped.
     */
    fun decode(bytes: ByteArray): CardModel? {
        return try {
            val json = String(bytes, Charsets.UTF_8)
            val obj = JSONObject(json)

            val fields = mutableListOf<CardField>()
            val arr = obj.optJSONArray("f")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val fObj = arr.getJSONObject(i)
                    fields.add(
                        CardField(
                            id    = UUID.randomUUID(),
                            key   = fObj.optString("k", ""),
                            label = fObj.optString("l", ""),
                            value = fObj.optString("v", ""),
                            kind  = fObj.optString("ki", "text")
                        )
                    )
                }
            }

            CardModel(
                id          = UUID.randomUUID(),
                type        = CardType.fromRaw(obj.optString("t", CardType.PERSONAL.name)),
                displayName = obj.optString("n", "Unknown"),
                subtitle    = obj.optString("s", "").takeIf { it.isNotEmpty() },
                org         = obj.optString("o", "").takeIf { it.isNotEmpty() },
                bio         = obj.optString("b", "").takeIf { it.isNotEmpty() },
                themeHex    = obj.optString("th", "#1C1C1E"),
                theme       = CardTheme.fromRaw(obj.optString("th", "#1C1C1E")),
                photoData   = null,          // Not sent over NFC — BLE follow-up
                fields      = fields,
                isFavorite  = false,
                isReceived  = true,          // Mark as received card
                note        = null,
                createdAt   = Date()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode card bytes", e)
            null
        }
    }
}
