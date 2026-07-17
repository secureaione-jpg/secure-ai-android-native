package one.secureai.app.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.secureai.app.R
import one.secureai.app.data.model.SubscriptionTier
import one.secureai.app.data.store.StoreManager
import one.secureai.app.ui.theme.Brand

private val UpgradeGold = Color(0xFFD9A621)

private data class TierFeature(val icon: Int, val text: String)

private data class TierCard(
    val name: String,
    val price: String,
    val period: String,
    val features: List<TierFeature>,
    val color: Color,
    val productId: String,
)

private val tiers = listOf(
    TierCard(
        name = "Plus",
        price = "$6.99",
        period = "/month",
        features = listOf(
            TierFeature(R.drawable.ic_new_chat, "50 AI chats per day"),
            TierFeature(R.drawable.ic_photos, "10 AI images per day"),
            TierFeature(R.drawable.ic_folder, "Unlimited Projects"),
        ),
        color = Brand,
        productId = "secureai_plus_monthly",
    ),
    TierCard(
        name = "Pro",
        price = "$24.99",
        period = "/month",
        features = listOf(
            TierFeature(R.drawable.ic_new_chat, "150 AI chats per day"),
            TierFeature(R.drawable.ic_photos, "50 AI images per day"),
            TierFeature(R.drawable.ic_folder, "Unlimited Projects"),
            TierFeature(R.drawable.ic_sparkle, "Advanced AI"),
        ),
        color = Color(0xFF8B5CF6),
        productId = "secureai_pro_monthly",
    ),
    TierCard(
        name = "Ultra",
        price = "$149.99",
        period = "/month",
        features = listOf(
            TierFeature(R.drawable.ic_new_chat, "300 AI chats per day"),
            TierFeature(R.drawable.ic_photos, "100 AI images per day"),
            TierFeature(R.drawable.ic_folder, "Unlimited Projects"),
            TierFeature(R.drawable.ic_sparkle, "Most Powerful AI"),
        ),
        color = UpgradeGold,
        productId = "secureai_ultra_monthly",
    ),
)

@Composable
fun PaywallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentTier by StoreManager.currentTier.collectAsState()
    val products by StoreManager.products.collectAsState()
    val isPurchasing by StoreManager.isPurchasing.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(24.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "Upgrade",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(24.dp))
            }

            // Title
            Spacer(Modifier.height(16.dp))
            Text(
                "Unlock the full power\nof Secure AI",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Choose the plan that works for you",
                fontSize = 15.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // Tier cards
            tiers.forEach { tier ->
                val isCurrentTier = when (tier.name) {
                    "Plus" -> currentTier == SubscriptionTier.PLUS
                    "Pro" -> currentTier == SubscriptionTier.PRO
                    "Ultra" -> currentTier == SubscriptionTier.ULTRA
                    else -> false
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1A1A1C),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                tier.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = tier.color
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                tier.price,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                tier.period,
                                fontSize = 14.sp,
                                color = Color(0xFFAEAEB2)
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        tier.features.forEach { feature ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(feature.icon),
                                    contentDescription = null,
                                    tint = tier.color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    feature.text,
                                    fontSize = 15.sp,
                                    color = Color(0xFFE5E5EA)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (isCurrentTier) {
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tier.color.copy(alpha = 0.2f),
                                    contentColor = tier.color
                                ),
                                enabled = false
                            ) {
                                Text("Current Plan", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val product = products.firstOrNull { p ->
                                        p.productId == tier.productId
                                    }
                                    if (product != null) {
                                        val offer = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
                                        if (offer != null && context is Activity) {
                                            StoreManager.purchase(context, product, offer)
                                        } else {
                                            Toast.makeText(context, "Unable to load subscription details. Please try again.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Unable to connect to Google Play. Please check your connection and try again.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = tier.color),
                                enabled = !isPurchasing
                            ) {
                                Text(
                                    "Subscribe",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Restore purchases
            TextButton(
                onClick = { StoreManager.restorePurchases() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore Purchases", color = Brand, fontSize = 15.sp)
            }

            // Free tier info
            Spacer(Modifier.height(8.dp))
            Text(
                "Free tier includes 10 messages/day and 2 images/day",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
        }

        // Back button clickable area (overlay top-left)
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, top = 16.dp)
        ) {
            TextButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Brand,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
