package com.example.cbtsmpdoabangsa.presentation.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {

    //  Preferences
    val USERNAME = stringPreferencesKey("username")
    val CLASS_TYPE = stringPreferencesKey("class_type")
    val EXAM_TYPE = booleanPreferencesKey("exam_type")
}