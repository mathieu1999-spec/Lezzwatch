package com.lezzwatch.app.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.lezzwatch.app.LezzwatchApplication

/**
 * Small helper so every ViewModel's `Factory` companion can grab the app-scoped [AppContainer]
 * in one line, instead of repeating the `CreationExtras` -> `Application` -> container dance in
 * every file. See any `*ViewModel.kt` for usage with `viewModelFactory { initializer { ... } }`.
 */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LezzwatchApplication).container
