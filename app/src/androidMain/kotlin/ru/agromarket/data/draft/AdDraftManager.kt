package ru.agromarket.data.draft

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.draftDataStore: DataStore<Preferences> by preferencesDataStore(name = "ad_draft_prefs")

/**
 * Create-ad wizard draft. Photo URIs are intentionally not persisted: the photo picker
 * grants transient read permissions that don't survive process death, so a restored
 * draft asks the user to re-attach photos instead of silently failing the upload.
 */
data class AdDraft(
    val type: String = "sale",
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val phonePrimary: String = "",
    val categoryId: Int? = null,
    val regionId: Int? = null,
    val regionName: String = "",
    val districtId: Int? = null,
    val districtName: String = "",
    val localityId: Int? = null,
    val localityName: String = "",
) {
    /** Whether the draft carries user-typed content worth restoring. */
    val isMeaningful: Boolean
        get() = title.isNotBlank() || description.isNotBlank() || price.isNotBlank() ||
            phonePrimary.isNotBlank() || categoryId != null
}

/** Persists the create-ad wizard draft so an interrupted submission can be resumed (Avito pattern). */
@Singleton
class AdDraftManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val DRAFT_JSON = stringPreferencesKey("create_ad_draft")
    }

    private val gson = Gson()

    suspend fun save(draft: AdDraft) {
        context.draftDataStore.edit { it[DRAFT_JSON] = gson.toJson(draft) }
    }

    suspend fun load(): AdDraft? = context.draftDataStore.data.first()[DRAFT_JSON]?.let { json ->
        try { gson.fromJson(json, AdDraft::class.java) } catch (_: Exception) { null }
    }

    suspend fun clear() {
        context.draftDataStore.edit { it.remove(DRAFT_JSON) }
    }
}
