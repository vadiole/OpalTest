package vadiole.opaltest.data.fake

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import vadiole.opaltest.data.debug.SimulatedLatency
import vadiole.opaltest.domain.referral.Plan

@Serializable
data class FakeServerState(
    @SerialName("plan") val plan: ServerPlan = ServerPlan.Free,
    @SerialName("successful_referrals") val successfulReferrals: Int = 0,
    @SerialName("claimed_tier_ids") val claimedTierIds: Set<String> = emptySet(),
    @SerialName("latency") val latency: SimulatedLatency = SimulatedLatency.None,
    @SerialName("fail_next_load") val failNextLoad: Boolean = false,
    @SerialName("fail_next_claim") val failNextClaim: Boolean = false
)

@Serializable
enum class ServerPlan {
    @SerialName("free")
    Free,

    @SerialName("pro")
    Pro;

    fun toPlan() = if (this == Pro) Plan.Pro else Plan.Free
}

fun Plan.toServerPlan() = if (this == Plan.Pro) ServerPlan.Pro else ServerPlan.Free

object FakeServerStateSerializer : Serializer<FakeServerState> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue = FakeServerState()

    override suspend fun readFrom(input: InputStream): FakeServerState = try {
        json.decodeFromString(input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unreadable fake server state", e)
    }

    override suspend fun writeTo(t: FakeServerState, output: OutputStream) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
