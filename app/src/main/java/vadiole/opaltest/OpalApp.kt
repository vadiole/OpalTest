package vadiole.opaltest

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import vadiole.opaltest.di.AppContainer
import vadiole.opaltest.di.DefaultAppContainer

class OpalApp : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}

val CreationExtras.container: AppContainer
    get() = (requireNotNull(this[APPLICATION_KEY]) as OpalApp).container
