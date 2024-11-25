# Real-time Developer Notifications

## Usage

## Setup

1. In GCP, create a service account
2. In Play Console, add necessary permissions to the service account
3. In GCP, create a topic and publisher (service account) in Pub/Sub
4. Host the server side verification logic in any way you want
5. Add subscription and pull the message from the API you created at 4.

## References

- https://developers.google.com/android-publisher
- https://developer.android.com/google/play/billing/rtdn-reference
- https://developer.android.com/google/play/billing/security#voided
- https://developer.android.com/google/play/billing/lifecycle
