package vadiole.opaltest.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import vadiole.opaltest.data.fake.FakeReferralApi
import vadiole.opaltest.data.fake.FakeReferralBackend
import vadiole.opaltest.data.fake.FakeServerStateSerializer
import vadiole.opaltest.data.fake.ReferralDebugControls
import vadiole.opaltest.data.referral.DefaultReferralRepository
import vadiole.opaltest.data.referral.ReferralRepository

interface AppContainer {
    val referralRepository: ReferralRepository
    val referralDebugControls: ReferralDebugControls
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val fakeReferralBackend by lazy {
        FakeReferralBackend(
            DataStoreFactory.create(
                serializer = FakeServerStateSerializer,
                corruptionHandler = ReplaceFileCorruptionHandler { FakeServerStateSerializer.defaultValue },
                produceFile = { context.dataStoreFile("fake_referral_server.json") }
            )
        )
    }

    override val referralRepository: ReferralRepository by lazy {
        DefaultReferralRepository(FakeReferralApi(fakeReferralBackend))
    }

    override val referralDebugControls: ReferralDebugControls get() = fakeReferralBackend
}
