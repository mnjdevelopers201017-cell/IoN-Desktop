import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object Supabase {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://ugrhlgmxzmgzshaznbdt.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVncmhsZ214em1nenNoYXpuYmR0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI1OTY2NDYsImV4cCI6MjEwODE3MjY0Nn0.0lJZ24Y2oQYH1GGsbqR3571ElRKiHjBbp1_f2wjLS58"
    ) {
        install(Auth) {
            alwaysAutoRefresh = true
        }
        install(Postgrest)
    }
}

@Serializable
data class Wallet(
    @SerialName("cash_balance") val cashBalance: Long = 0,
    @SerialName("crypto_balance") val cryptoBalance: Long = 0
)

@Serializable
data class Job(
    val id: String? = null,
    val title: String,
    val description: String,
    val reward: String,
    @SerialName("publisher_id") val publisherId: String? = null,
    @SerialName("publisher_name") val publisherName: String? = null,
    @SerialName("coin_type") val coinType: String = "cash",
    @SerialName("total_slots") val totalSlots: Int = 1,
    @SerialName("completed_slots") val completedSlots: Int = 0,
    @SerialName("device_type") val deviceType: String = "desktop"
)
