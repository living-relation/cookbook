package com.livingrelation.markdownnotes.core.files

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

interface RecentsRepository {
    fun observeRecents(): Flow<List<RecentNote>>

    suspend fun touch(uri: Uri, displayName: String)

    suspend fun remove(uri: Uri)

    suspend fun clear()

    suspend fun validateAndPrune(context: Context)
}

private val Context.recentsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "markdown_notes_recents",
)

class DataStoreRecentsRepository(
    private val context: Context,
) : RecentsRepository {
    override fun observeRecents(): Flow<List<RecentNote>> =
        context.recentsDataStore.data.map { preferences ->
            decodeRecents(preferences[RECENTS_JSON_KEY].orEmpty())
        }

    override suspend fun touch(uri: Uri, displayName: String) {
        context.recentsDataStore.edit { preferences ->
            val current = decodeRecents(preferences[RECENTS_JSON_KEY].orEmpty())
            val updated =
                RecentNotesLogic.touch(
                    current = current,
                    uriString = uri.toString(),
                    displayName = displayName,
                )
            preferences[RECENTS_JSON_KEY] = encodeRecents(updated)
        }
    }

    override suspend fun remove(uri: Uri) {
        context.recentsDataStore.edit { preferences ->
            val current = decodeRecents(preferences[RECENTS_JSON_KEY].orEmpty())
            preferences[RECENTS_JSON_KEY] =
                encodeRecents(RecentNotesLogic.remove(current, uri.toString()))
        }
    }

    override suspend fun clear() {
        context.recentsDataStore.edit { preferences ->
            preferences.remove(RECENTS_JSON_KEY)
        }
    }

    override suspend fun validateAndPrune(context: Context) {
        context.recentsDataStore.edit { preferences ->
            val current = decodeRecents(preferences[RECENTS_JSON_KEY].orEmpty())
            val kept =
                current.filter { note ->
                    val uri = runCatching { Uri.parse(note.uriString) }.getOrNull()
                    uri != null && NoteIo.isUriAccessible(context, uri)
                }
            preferences[RECENTS_JSON_KEY] = encodeRecents(kept)
        }
    }

    companion object {
        private val RECENTS_JSON_KEY = stringPreferencesKey("recent_notes_json")

        fun create(context: Context): RecentsRepository =
            DataStoreRecentsRepository(context.applicationContext)
    }
}

private fun encodeRecents(notes: List<RecentNote>): String {
    val array = JSONArray()
    for (note in notes) {
        array.put(
            JSONObject()
                .put("uri", note.uriString)
                .put("name", note.displayName)
                .put("openedAt", note.lastOpenedAtEpochMs),
        )
    }
    return array.toString()
}

private fun decodeRecents(json: String): List<RecentNote> {
    if (json.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RecentNote(
                        uriString = item.getString("uri"),
                        displayName = item.getString("name"),
                        lastOpenedAtEpochMs = item.getLong("openedAt"),
                    ),
                )
            }
        }
    }.getOrElse { emptyList() }
}
