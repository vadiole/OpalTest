package vadiole.opaltest.di

import android.content.Context

interface AppContainer

class DefaultAppContainer(private val context: Context) : AppContainer
