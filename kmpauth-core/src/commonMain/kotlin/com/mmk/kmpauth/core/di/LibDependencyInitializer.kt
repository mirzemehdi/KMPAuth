package com.mmk.kmpauth.core.di


import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module


internal object LibDependencyInitializer {
    var koinApp: KoinApplication? = null
        private set


    fun initialize() {
        if (isInitialized()) return
        val configModule = module {

        }
        koinApp = koinApplication {
            modules(configModule + platformModule)
        }.also {
            it.koin.onLibraryInitialized()
        }

    }

    private fun isInitialized() = koinApp != null


}

private fun Koin.onLibraryInitialized() {
    println("Library is initialized")
}

