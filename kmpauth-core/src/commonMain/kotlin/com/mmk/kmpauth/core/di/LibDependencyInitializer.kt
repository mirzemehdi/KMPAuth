package com.mmk.kmpauth.core.di


import com.mmk.kmpauth.core.KMPAuthInternalApi
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@KMPAuthInternalApi
public object LibDependencyInitializer {

    @KMPAuthInternalApi
    public var koinApp: KoinApplication? = null
        private set

    @KMPAuthInternalApi
    public fun initialize(module: Module) {
        initialize(listOf(module))
    }

    @KMPAuthInternalApi
    public fun initialize(modules: List<Module> = emptyList()) {
        if (isInitialized()) return
        val configModule = module {
            includes(modules)
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

