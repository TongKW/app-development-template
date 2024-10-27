import androidx.activity.ComponentActivity
import android.os.Bundle
import com.yourapp.manager.BillingManager
import com.yourapp.navigation.NavGraph
import com.yourapp.ui.theme.YourAppTheme




class MainActivity : ComponentActivity() {
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YourAppTheme {
                val context = LocalContext.current
                billingManager = remember { BillingManager(context) }

                // Or replace with your own implementation
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    billingManager = billingManager,
                )
            }
        }
    }
}

