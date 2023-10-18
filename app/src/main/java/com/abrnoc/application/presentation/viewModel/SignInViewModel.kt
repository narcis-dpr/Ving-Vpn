package com.abrnoc.application.presentation.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrnoc.application.presentation.mapper.mapToDomain
import com.abrnoc.application.presentation.viewModel.event.SignInEvent
import com.abrnoc.application.presentation.viewModel.state.SignInState
import com.abrnoc.application.presentation.viewModel.state.model.SignInObj
import com.abrnoc.domain.auth.SignInPasswordUseCase
import com.abrnoc.domain.common.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInPasswordUseCase: SignInPasswordUseCase
) : ViewModel() {
//    var state by mutableStateOf(SignInState())
private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state
    fun onEvent(event: SignInEvent) {
        when (event) {
            is SignInEvent.SignInQuery -> {
                _state.value = SignInState(signIn = SignInObj(event.email, event.password), isRequestSend = true)
                requestSignIn()

            }
        }
    }

    private fun requestSignIn() {
        viewModelScope.launch {
            signInPasswordUseCase(_state.value.signIn!!.mapToDomain()).collect { result ->
                when (result) {

                    is Result.Error -> {
                        _state.value = SignInState(isLoading = false, error = result.exception.toString(), isSuccessful = false)
                    }

                    Result.Loading -> {
                        _state.value = SignInState(isLoading = true)
                    }

                    is Result.Success -> {
                        _state.value = SignInState(isLoading = false, isSuccessful = true)
                    }
                }
            }

        }

    }

}

