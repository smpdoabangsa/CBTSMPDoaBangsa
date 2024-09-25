package com.example.cbtsmpdoabangsa.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(): ViewModel() {

    private val _linkState = MutableStateFlow(value = "")
    val linkState: StateFlow<String> = _linkState

    fun updateLink(link: String) {
        _linkState.update { link }
    }

    private val _canGoBackState = MutableStateFlow(value = false)
    val canGoBackState: StateFlow<Boolean> = _canGoBackState

    fun updateBackActionAvailable(available: Boolean) {
        if (_canGoBackState.value == available) return
        _canGoBackState.update { available }
    }
}