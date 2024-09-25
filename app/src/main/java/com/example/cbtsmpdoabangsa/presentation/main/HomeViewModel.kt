package com.example.cbtsmpdoabangsa.presentation.main

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cbtsmpdoabangsa.presentation.util.Constants.CLASS_TYPE
import com.example.cbtsmpdoabangsa.presentation.util.Constants.EXAM_TYPE
import com.example.cbtsmpdoabangsa.presentation.util.Constants.USERNAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: DataStore<Preferences>
): ViewModel() {

    val username = userPreferences.data.map { pref: Preferences ->
        pref[USERNAME] ?: ""
    }

    val userClass = userPreferences.data.map { pref: Preferences ->
        pref[CLASS_TYPE] ?: ""
    }

    val examType = userPreferences.data.map { pref: Preferences ->
        pref[EXAM_TYPE] ?: false
    }

    fun updateUsername(
        username: String,
        userClassType: String,
        examType: Boolean
    ) = viewModelScope.launch {
        userPreferences.edit { preferences ->
            preferences[USERNAME] = username
            preferences[CLASS_TYPE] = userClassType
            preferences[EXAM_TYPE] = examType
        }
    }


}