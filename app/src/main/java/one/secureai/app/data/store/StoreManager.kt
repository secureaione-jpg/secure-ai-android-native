package one.secureai.app.data.store

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import one.secureai.app.data.model.SubscriptionTier

object StoreManager : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null

    private val _currentTier = MutableStateFlow(SubscriptionTier.FREE)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    val productIds = listOf(
        "secureai_plus_monthly",
        "secureai_plus_yearly",
        "secureai_pro_monthly",
        "secureai_pro_yearly",
        "secureai_ultra_monthly"
    )

    fun initialize(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productIds.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            })
            .build()

        billingClient?.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = details
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient?.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchase = purchases.firstOrNull {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                updateTierFromPurchase(activePurchase)
            }
        }
    }

    fun purchase(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        _isPurchasing.value = true
        // Ties this purchase to the Firebase uid server-side, so the backend's
        // Real-time Developer Notifications webhook can attribute the purchase
        // without waiting on syncToFirestore's client-side write to land first.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val paramsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            ))
        if (uid != null) paramsBuilder.setObfuscatedAccountId(uid)
        billingClient?.launchBillingFlow(activity, paramsBuilder.build())
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _isPurchasing.value = false
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            val purchase = purchases.firstOrNull {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            updateTierFromPurchase(purchase)
            purchase?.let { syncToFirestore(it) }
        }
    }

    fun restorePurchases() {
        queryPurchases()
    }

    private fun updateTierFromPurchase(purchase: Purchase?) {
        if (purchase == null) {
            _currentTier.value = SubscriptionTier.FREE
            return
        }
        val productId = purchase.products.firstOrNull() ?: ""
        _currentTier.value = when {
            productId.contains("ultra") -> SubscriptionTier.ULTRA
            productId.contains("pro") -> SubscriptionTier.PRO
            productId.contains("plus") -> SubscriptionTier.PLUS
            else -> SubscriptionTier.FREE
        }
    }

    private fun syncToFirestore(purchase: Purchase) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val data = hashMapOf<String, Any>(
            "googlePlayOrderId" to (purchase.orderId ?: ""),
            "googlePlayProductId" to (purchase.products.firstOrNull() ?: ""),
            // Fallback key the backend's RTDN webhook uses to resolve this purchase
            // to a uid if it arrives before setObfuscatedAccountId's value is
            // queryable via the Play Developer API (a race on first purchase).
            "googlePlayPurchaseToken" to purchase.purchaseToken,
            "platform" to "android"
        )
        FirebaseFirestore.getInstance()
            .collection("shared_users").document(uid)
            .set(data, SetOptions.merge())
    }

    fun reset() {
        _currentTier.value = SubscriptionTier.FREE
        _products.value = emptyList()
    }
}
