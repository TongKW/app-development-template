import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun PaymentTriggerButton(billingManager: BillingManager) {
    val context = LocalContext.current
    Button(
        onClick = {
            (context as? Activity)?.let {
                billingManager.launchPurchaseFlow(
                    activity = it,
                    sku = "<your_product_id>"
                )
            }
        }
    ) {
        Text(text = "Purchase item")
    }
}