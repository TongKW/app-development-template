import Foundation
import StoreKit

func subscribeToProduct(productId: String) async -> (productId: String?, transactionId: String?, originalTransactionId: String?) {
    print("Subscription process initiated.")
    
    do {
        // Fetch the product using the provided product ID
        let products = try await Product.products(for: [productId])
        guard let product = products.first else {
            print("Error: Unable to fetch the product.")
            return (nil, nil, nil)
        }
        
        // Attempt to purchase the product
        let result = try await product.purchase()
        
        switch result {
        case let .success(.verified(transaction)):
            print("Transaction successful.")
            await transaction.finish()
            return (product.id, String(transaction.id), String(transaction.originalID))
            
        case let .success(.unverified(_, error)):
            print("Transaction unverified: \(error.localizedDescription)")
            return (nil, nil, nil)
            
        case .pending:
            print("Transaction is pending.")
            return (nil, nil, nil)
            
        case .userCancelled:
            print("Transaction was cancelled by the user.")
            return (nil, nil, nil)
            
        @unknown default:
            print("Unknown transaction state.")
            return (nil, nil, nil)
        }
    } catch {
        print("Purchase failed with error: \(error.localizedDescription)")
        return (nil, nil, nil)
    }
}
