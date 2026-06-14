package com.kflix.app

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import java.security.Security
import org.conscrypt.Conscrypt
import com.kflix.app.database.AppDatabase
import com.kflix.app.providers.AniWorldProvider
import com.kflix.app.providers.SerienStreamProvider
import com.kflix.app.utils.AppLanguageManager
import com.kflix.app.utils.ArtworkRepairScheduler
import com.kflix.app.utils.CacheUtils
import com.kflix.app.utils.DnsResolver
import com.kflix.app.utils.IsrgRootTrustProvider
import com.kflix.app.utils.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KflixApp : Application() {
    companion object {
        lateinit var instance: KflixApp
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguageManager.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Security.insertProviderAt(Conscrypt.newProvider(), 1)

        IsrgRootTrustProvider.install()

        UserPreferences.setup(this)
        DnsResolver.setDnsUrl(UserPreferences.dohProviderUrl)

        val appContext = applicationContext
        val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val threshold = if (isTv) 10L else 50L

        applicationScope.launch(Dispatchers.IO) {
            AppDatabase.setup(appContext)
            SerienStreamProvider.initialize(appContext)
            AniWorldProvider.initialize(appContext)
            ArtworkRepairScheduler.schedule(appContext, UserPreferences.currentProvider)
            CacheUtils.autoClearIfNeeded(appContext, thresholdMb = threshold)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            CacheUtils.clearAppCache(this)
        }
    }
}
