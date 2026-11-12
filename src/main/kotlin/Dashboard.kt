import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun DashboardScreen() {
    var cashBalance by remember { mutableStateOf(0L) }
    var cryptoBalance by remember { mutableStateOf(0L) }
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var statusText by remember { mutableStateOf("Loading...") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            // Check if the user actually has a valid session
            val session = Supabase.client.auth.currentSessionOrNull()
            if (session == null) {
                statusText = "⚠️ Not logged in! Please check your email to confirm your account, then log in."
                return@launch
            }

            try {
                // Fetch Wallet
                val wallet = Supabase.client.postgrest["wallets"].select().decodeSingleOrNull<Wallet>()
                if (wallet != null) {
                    cashBalance = wallet.cashBalance
                    cryptoBalance = wallet.cryptoBalance
                }

                // Fetch Jobs
                jobs = Supabase.client.postgrest["jobs"].select().decodeList<Job>()
                statusText = "Connected securely as ${session.user?.email}"
            } catch (e: Exception) {
                statusText = "Error: ${e.message}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text("IoN Dashboard", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(statusText, color = if (statusText.contains("⚠️") || statusText.contains("Error")) Color.Red else Color.Green, fontSize = 14.sp)
        
        Spacer(Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                Column(Modifier.padding(24.dp)) {
                    Text("Cash Balance", color = Color.Gray)
                    Text("$cashBalance Coins", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                Column(Modifier.padding(24.dp)) {
                    Text("Crypto Balance", color = Color.Gray)
                    Text("$cryptoBalance Coins", color = Color(0xFF4285F4), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("Available Jobs", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(jobs) { job ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(job.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(job.description, color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reward: ${job.reward} ${job.coinType}", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)
                            Button(onClick = {
                                scope.launch {
                                    if (Supabase.client.auth.currentSessionOrNull() == null) {
                                        statusText = "⚠️ Session expired. Please log in again."
                                        return@launch
                                    }
                                    try {
                                        Supabase.client.postgrest.rpc(
                                            "complete_job",
                                            buildJsonObject {
                                                put("job_id", job.id)
                                                put("coin_type", job.coinType)
                                                put("reward_amount", job.reward.toIntOrNull() ?: 0)
                                            }
                                        )
                                        statusText = "✅ Job completed securely!"
                                    } catch (e: Exception) {
                                        statusText = "RPC Error: ${e.message}"
                                    }
                                }
                            }) { Text("Complete Job") }
                        }
                    }
                }
            }
        }
    }
}
