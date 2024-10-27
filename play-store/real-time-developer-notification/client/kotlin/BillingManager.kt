package build.designand.riselife.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetailsParams
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.json.JSONObject
import java.io.IOException

data class PurchaseData(
    val productId: String?,
    val transactionId: String?,
    val originalTransactionId: String?
)

class BillingManager(context: Context) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val billingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases()
        .setListener { billingResult, purchases ->
            Log.d("YourApp [BillingManager]", "billingResult: $billingResult")
            Log.d("YourApp [BillingManager]", "purchases: $purchases")

            // Handle purchase updates here
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
        .build()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // BillingClient is ready
                }
            }
            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to Google Play
            }
        })
    }

    fun launchPurchaseFlow(activity: Activity, sku: String, isSubscription: Boolean) {
        val skuList = listOf(sku)
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(if (isSubscription) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP)
            .build()

        billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetailsList[0])
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    // Assuming purchase only has a single product
    private fun handlePurchase(purchase: Purchase) {
        Log.d("YourApp [BillingManager]", "Handling purchase: $purchase")

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val purchaseData = PurchaseData(
                            productId = purchase.products.firstOrNull(),
                            transactionId = purchase.orderId,
                            originalTransactionId = purchase.purchaseToken
                        )
                        if (purchaseData.productId != null) {
                            // Update user purchase record to your database so that it can be verified by notifications handler later
                            // eg: updateUserTransactionInfo(purchaseData)
                        } else {
                            // Handle fallback here
                        }
                    }
                }
            } else {
                val purchaseData = PurchaseData(
                    productId = purchase.products.firstOrNull(),
                    transactionId = purchase.orderId,
                    originalTransactionId = purchase.purchaseToken
                )
                if (purchaseData.productId != null) {
                    // Update user purchase record to your database so that it can be verified by notifications handler later
                    // eg: updateUserTransactionInfo(purchaseData)
                } else {
                    // Handle fallback here
                }
            }
        }
    }
}