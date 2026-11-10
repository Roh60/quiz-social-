# Payout and admin guide

Coins are written by the app, so a cheater can fake a balance. Payouts are manual. Check before you pay.

## Before paying any withdrawal request
1. Firebase Console, Firestore, collection withdrawalRequests. Open the PENDING request, note userId and upiId.
2. Open wallets/<userId> and check totalEarnedLifetimeCoins.
3. In coinTransactions, filter by that userId. The history must match the balance
   (check-ins, posts, quiz and answer rewards). Reject if the balance has no matching history.
4. Reject accounts created minutes ago, or with hundreds of rewards within a few minutes.
5. Pay the UPI ID from your own app, then mark the request PAID in the admin screen with the UTR number.
6. Rejecting in the admin screen returns the coins to the user.

## Limits
Minimum Rs 10, maximum Rs 50 per user per day. To stop all new requests set
isWithdrawalPaused = true in earningConfig/global.

## Admin login
Only rohitpandit55970@gmail.com with a verified email. Verify it with Forgot Password
(open the mail link) or sign in with Google.

## Real protection (next project)
Move coin crediting and withdrawals to Cloud Functions. Needs: Firebase Blaze plan, a deploy
secret in GitHub, and moving the wallet code to the server. Then lock wallets and coinTransactions
writes in firestore.rules.
