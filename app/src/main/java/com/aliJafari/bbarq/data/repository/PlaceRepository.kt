package com.aliJafari.bbarq.data.repository

import android.content.Context
import androidx.core.content.edit
import com.aliJafari.bbarq.R
import com.aliJafari.bbarq.data.local.ADatabase
import com.aliJafari.bbarq.data.model.Place

class PlaceRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = ADatabase.getInstance(appContext).PlaceDao()
    private val prefs = appContext.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

    fun getPlaces(): List<Place> {
        migrateLegacyBillId()
        return dao.getAll()
    }

    fun savePlace(place: Place): Place {
        return if (place.id == 0L) {
            val id = dao.insert(place)
            place.copy(id = id)
        } else {
            dao.update(place)
            place
        }
    }

    fun deletePlace(place: Place) {
        dao.delete(place)
    }

    private fun migrateLegacyBillId() {
        if (prefs.getBoolean(LEGACY_BILL_ID_MIGRATED, false)) return

        val existingPlaces = dao.getAll()
        val legacyBillId = prefs.getString("billId", "").orEmpty()
        if (existingPlaces.isEmpty() && legacyBillId.length == BILL_ID_LENGTH) {
            dao.insert(
                Place(
                    name = appContext.getString(R.string.default_place_name),
                    billId = legacyBillId,
                    colorKey = "red",
                    iconKey = "home"
                )
            )
        }
        prefs.edit(commit = true) { putBoolean(LEGACY_BILL_ID_MIGRATED, true) }
    }

    companion object {
        private const val BILL_ID_LENGTH = 13
        private const val LEGACY_BILL_ID_MIGRATED = "legacy_bill_id_migrated"
    }
}
