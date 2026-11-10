package com.example.data.remote

import android.util.Log
import com.example.data.model.CoinTransaction
import com.example.data.model.EarningConfig
import com.example.data.model.UserWallet
import com.example.data.model.WithdrawalRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.example.data.model.DailyCheckinRecord
import com.example.data.model.DailyChallengeRecord
import com.example.data.model.AnswerRewardRecord
import com.example.data.model.PostRewardRecord
import com.example.data.model.RewardedAdClaimRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repository for secure financial ledger transactions, wallet balances, and withdrawal operations.
 */
class WalletRepository(
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

  private val auth = Firebase.auth
  private val todayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

  private val ADMIN_EMAILS = setOf(
    "rohitpandit55970@gmail.com",
    "admin@quizpoll.app"
  )

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in for wallet transactions.")
  }

  fun isCurrentUserAdmin(): Boolean {
    val email = auth.currentUser?.email?.lowercase()?.trim() ?: ""
    return ADMIN_EMAILS.contains(email)
  }

  // ---------------- WALLET OBSERVATION ----------------

  fun observeUserWallet(userId: String): Flow<UserWallet> = flow {
    emitAll(
      db.collection("wallets").document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
          } else {
            UserWallet(userId = userId)
          }
        }
        .catch { e ->
          Log.w("WalletRepository", "Error observing wallet for $userId: ${e.message}")
          emit(UserWallet(userId = userId))
        }
    )
  }

  suspend fun getOrCreateWallet(userId: String): UserWallet {
    val docRef = db.collection("wallets").document(userId)
    val snap = docRef.get().await()
    if (snap.exists()) {
      return snap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
    }

    val initial = UserWallet(
      userId = userId,
      cashConvertibleCoins = 0L,
      appOnlyRewardPoints = 0L,
      lastEarningDate = todayDateFormat.format(Date())
    )
    docRef.set(initial, SetOptions.merge()).await()
    return initial
  }

  // ---------------- EARNING CONFIGURATION ----------------

  fun observeEarningConfig(): Flow<EarningConfig> = flow {
    emitAll(
      db.collection("earningConfig").document("global")
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(EarningConfig::class.java) ?: EarningConfig()
          } else {
            EarningConfig()
          }
        }
        .catch { emit(EarningConfig()) }
    )
  }

  suspend fun getEarningConfig(): EarningConfig {
    return try {
      val snap = db.collection("earningConfig").document("global").get().await()
      if (snap.exists()) snap.toObject(EarningConfig::class.java) ?: EarningConfig() else EarningConfig()
    } catch (_: Exception) {
      EarningConfig()
    }
  }

  suspend fun updateEarningConfig(newConfig: EarningConfig): Result<Unit> = runCatching {
    if (!isCurrentUserAdmin()) {
      throw IllegalAccessException("Admin privileges required to modify earning configuration.")
    }
    db.collection("earningConfig").document("global").set(newConfig, SetOptions.merge()).await()
  }

  // ---------------- TRANSACTIONS OBSERVATION ----------------

  fun observeUserTransactions(userId: String): Flow<List<CoinTransaction>> = flow {
    emitAll(
      db.collection("coinTransactions")
        .whereEqualTo("userId", userId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(100)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(CoinTransaction::class.java)
        }
        .catch { e ->
          Log.w("WalletRepository", "Error observing transactions for $userId: ${e.message}")
          emit(emptyList())
        }
    )
  }

  // ---------------- WITHDRAWALS OBSERVATION ----------------

  fun observeUserWithdrawals(userId: String): Flow<List<WithdrawalRequest>> = flow {
    emitAll(
      db.collection("withdrawalRequests")
        .whereEqualTo("userId", userId)
        .orderBy("requestedAt", Query.Direction.DESCENDING)
        .limit(50)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(WithdrawalRequest::class.java)
        }
        .catch { emit(emptyList()) }
    )
  }

  fun observeAllWithdrawalsForAdmin(statusFilter: String? = null): Flow<List<WithdrawalRequest>> = flow {
    var query: Query = db.collection("withdrawalRequests").orderBy("requestedAt", Query.Direction.DESCENDING).limit(100)
    if (!statusFilter.isNullOrBlank() && statusFilter != "ALL") {
      query = db.collection("withdrawalRequests")
        .whereEqualTo("status", statusFilter)
        .orderBy("requestedAt", Query.Direction.DESCENDING)
        .limit(100)
    }

    emitAll(
      query.snapshots()
        .map { snapshot -> snapshot.toObjects(WithdrawalRequest::class.java) }
        .catch { e ->
          Log.w("WalletRepository", "Error observing admin withdrawals: ${e.message}")
          emit(emptyList())
        }
    )
  }

  // ---------------- EARNING CREDITS ----------------

  /**
   * Credits cash-convertible coins for successfully completing a quiz with score >= 70%.
   */
  suspend fun creditQuizWinCoins(
    userId: String,
    quizId: String,
    quizTitle: String,
    score: Int,
    total: Int
  ): Result<Long> = runCatching {
    if (total <= 0 || (score.toDouble() / total.toDouble()) < 0.7) {
      return@runCatching 0L
    }

    val config = getEarningConfig()
    val rewardCoins = config.quizWinCoins
    val idempotencyKey = "quiz_win_${quizId}_${userId}"

    // Check if this quiz completion reward was already awarded
    val existingTx = db.collection("coinTransactions")
      .whereEqualTo("idempotencyKey", idempotencyKey)
      .limit(1)
      .get()
      .await()

    if (!existingTx.isEmpty) {
      Log.d("WalletRepository", "Reward already claimed for $idempotencyKey")
      return@runCatching 0L
    }

    val todayStr = todayDateFormat.format(Date())
    val walletRef = db.collection("wallets").document(userId)
    val txRef = db.collection("coinTransactions").document()

    val paisa = config.calculateInrPaise(rewardCoins)
    val tx = CoinTransaction(
      transactionId = txRef.id,
      userId = userId,
      type = "CREDIT",
      source = "QUIZ_REWARD",
      coinAmount = rewardCoins,
      paisaEquivalent = paisa,
      isCashConvertible = true,
      description = "Knowledge Master Reward: $quizTitle ($score/$total)",
      idempotencyKey = idempotencyKey,
      status = "COMPLETED"
    )

    db.runTransaction { firestoreTx ->
      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val newToday = if (currentWallet.lastEarningDate == todayStr) {
        currentWallet.todayEarnedCoins + rewardCoins
      } else {
        rewardCoins
      }

      val updatedWallet = currentWallet.copy(
        cashConvertibleCoins = currentWallet.cashConvertibleCoins + rewardCoins,
        totalEarnedLifetimeCoins = currentWallet.totalEarnedLifetimeCoins + rewardCoins,
        todayEarnedCoins = newToday,
        lastEarningDate = todayStr
      )

      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    rewardCoins
  }

  /**
   * Credits cash-convertible coins for voting in a community poll discussion.
   */
  suspend fun creditPollVoteCoins(
    userId: String,
    pollId: String,
    pollQuestion: String
  ): Result<Long> = runCatching {
    val config = getEarningConfig()
    val rewardCoins = config.pollVoteCoins
    val idempotencyKey = "poll_vote_${pollId}_${userId}"

    val existingTx = db.collection("coinTransactions")
      .whereEqualTo("idempotencyKey", idempotencyKey)
      .limit(1)
      .get()
      .await()

    if (!existingTx.isEmpty) {
      return@runCatching 0L
    }

    val todayStr = todayDateFormat.format(Date())
    val walletRef = db.collection("wallets").document(userId)
    val txRef = db.collection("coinTransactions").document()

    val paisa = config.calculateInrPaise(rewardCoins)
    val tx = CoinTransaction(
      transactionId = txRef.id,
      userId = userId,
      type = "CREDIT",
      source = "POLL_REWARD",
      coinAmount = rewardCoins,
      paisaEquivalent = paisa,
      isCashConvertible = true,
      description = "Community Participation: \"${pollQuestion.take(30)}\"",
      idempotencyKey = idempotencyKey,
      status = "COMPLETED"
    )

    db.runTransaction { firestoreTx ->
      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val newToday = if (currentWallet.lastEarningDate == todayStr) {
        currentWallet.todayEarnedCoins + rewardCoins
      } else {
        rewardCoins
      }

      val updatedWallet = currentWallet.copy(
        cashConvertibleCoins = currentWallet.cashConvertibleCoins + rewardCoins,
        totalEarnedLifetimeCoins = currentWallet.totalEarnedLifetimeCoins + rewardCoins,
        todayEarnedCoins = newToday,
        lastEarningDate = todayStr
      )

      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    rewardCoins
  }

  /**
   * Credits strictly non-cash App-Only Reward Points from Google AdMob rewarded ads.
   *
   * STRICT POLICY ENFORCEMENT:
   * - Points earned here are added to appOnlyRewardPoints.
   * - Never added to cashConvertibleCoins or convertible to UPI/cash.
   */
  suspend fun creditAdMobAppPoints(
    userId: String,
    pointsAmount: Long,
    idempotencyKey: String
  ): Result<Long> = runCatching {
    val config = getEarningConfig()
    if (!config.isAdMobEnabled || !config.isAdMobRewardedAllowed) {
      throw IllegalStateException("AdMob rewarded challenges are currently disabled.")
    }

    val walletRef = db.collection("wallets").document(userId)
    val txRef = db.collection("coinTransactions").document()

    val tx = CoinTransaction(
      transactionId = txRef.id,
      userId = userId,
      type = "REWARD",
      source = "ADMOB_APP_POINTS",
      coinAmount = pointsAmount,
      paisaEquivalent = 0L, // Non-cash in-app points per AdMob policy
      isCashConvertible = false,
      description = "AdMob Community Challenge (App-Only Bonus Points)",
      idempotencyKey = idempotencyKey,
      status = "COMPLETED"
    )

    db.runTransaction { firestoreTx ->
      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val updatedWallet = currentWallet.copy(
        appOnlyRewardPoints = currentWallet.appOnlyRewardPoints + pointsAmount
      )

      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    pointsAmount
  }

  /**
   * Credits InMobi rewarded points / coins.
   *
   * COMPLIANCE RULE:
   * If isInMobiCashConversionAllowed is false (default), points are credited
   * as non-transferable app points. Only if admin explicitly enabled it is it convertible.
   */
  suspend fun creditInMobiReward(
    userId: String,
    coinsAmount: Long,
    idempotencyKey: String
  ): Result<Long> = runCatching {
    val config = getEarningConfig()
    if (!config.isInMobiEnabled) {
      throw IllegalStateException("InMobi network is currently disabled.")
    }

    val isConvertible = config.isInMobiCashConversionAllowed
    val walletRef = db.collection("wallets").document(userId)
    val txRef = db.collection("coinTransactions").document()

    val paisa = if (isConvertible) config.calculateInrPaise(coinsAmount) else 0L

    val tx = CoinTransaction(
      transactionId = txRef.id,
      userId = userId,
      type = "REWARD",
      source = "INMOBI_OFFER",
      coinAmount = coinsAmount,
      paisaEquivalent = paisa,
      isCashConvertible = isConvertible,
      description = if (isConvertible) "InMobi Sponsored Reward" else "InMobi Bonus Points (In-App Only)",
      idempotencyKey = idempotencyKey,
      status = "COMPLETED"
    )

    db.runTransaction { firestoreTx ->
      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val updatedWallet = if (isConvertible) {
        val todayStr = todayDateFormat.format(Date())
        val newToday = if (currentWallet.lastEarningDate == todayStr) {
          currentWallet.todayEarnedCoins + coinsAmount
        } else {
          coinsAmount
        }
        currentWallet.copy(
          cashConvertibleCoins = currentWallet.cashConvertibleCoins + coinsAmount,
          totalEarnedLifetimeCoins = currentWallet.totalEarnedLifetimeCoins + coinsAmount,
          todayEarnedCoins = newToday,
          lastEarningDate = todayStr
        )
      } else {
        currentWallet.copy(
          appOnlyRewardPoints = currentWallet.appOnlyRewardPoints + coinsAmount
        )
      }

      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    coinsAmount
  }

  // ---------------- WITHDRAWAL PROCESSING ----------------

  /**
   * Submits a withdrawal request using verified UPI ID.
   *
   * SECURE ATOMIC VALIDATION:
   * 1. Checks that withdrawal is not paused by admin.
   * 2. Checks coinAmount >= minWithdrawalCoins.
   * 3. Validates UPI ID regex syntax.
   * 4. Atomically checks cashConvertibleCoins >= coinAmount.
   * 5. Atomically debits cashConvertibleCoins and reserves into pendingWithdrawalCoins.
   * 6. Creates immutable transaction ledger and withdrawal request record.
   */
  suspend fun submitWithdrawalRequest(
    userId: String,
    upiId: String,
    requestedCoins: Long
  ): Result<String> = runCatching {
    val cleanUpi = upiId.trim()
    // Standard UPI ID validation: e.g. user@bank, mobile@paytm
    val upiRegex = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9]{2,64}$")
    if (!upiRegex.matches(cleanUpi)) {
      throw IllegalArgumentException("Please enter a valid UPI ID (e.g. yourname@oksbi or 9876543210@paytm).")
    }

    val config = getEarningConfig()
    if (config.isWithdrawalPaused) {
      throw IllegalStateException(config.pauseReason.ifBlank { "Withdrawals are temporarily paused for routine platform maintenance." })
    }

    if (requestedCoins < config.minWithdrawalCoins) {
      val minInr = config.calculateInrRupees(config.minWithdrawalCoins)
      throw IllegalArgumentException("Minimum withdrawal is ${config.minWithdrawalCoins} coins (₹${minInr.toInt()}).")
    }

    val user = auth.currentUser ?: throw IllegalStateException("User not signed in.")
    val userEmail = user.email ?: ""
    val userName = user.displayName ?: "QuizPoll Member"

    val amountInPaise = config.calculateInrPaise(requestedCoins)
    val amountInRupees = amountInPaise / 100.0

    val walletRef = db.collection("wallets").document(userId)
    val requestRef = db.collection("withdrawalRequests").document()
    val txRef = db.collection("coinTransactions").document()

    val withdrawal = WithdrawalRequest(
      requestId = requestRef.id,
      userId = userId,
      userEmail = userEmail,
      userName = userName,
      upiId = cleanUpi,
      requestedCoins = requestedCoins,
      amountInPaise = amountInPaise,
      amountInRupees = amountInRupees,
      status = "PENDING"
    )

    val tx = CoinTransaction(
      transactionId = txRef.id,
      userId = userId,
      type = "WITHDRAWAL_HOLD",
      source = "WITHDRAWAL",
      coinAmount = requestedCoins,
      paisaEquivalent = amountInPaise,
      isCashConvertible = true,
      description = "Withdrawal request reserved for UPI $cleanUpi (Ref: ${requestRef.id.take(8)})",
      idempotencyKey = "wd_req_${requestRef.id}",
      status = "PENDING"
    )

    db.runTransaction { firestoreTx ->
      val walletSnap = firestoreTx.get(walletRef)
      if (!walletSnap.exists()) {
        throw IllegalStateException("User wallet record not found.")
      }
      val currentWallet = walletSnap.toObject(UserWallet::class.java)
        ?: throw IllegalStateException("Could not parse wallet.")

      if (currentWallet.cashConvertibleCoins < requestedCoins) {
        throw IllegalArgumentException("Insufficient cash-convertible coins. Available: ${currentWallet.cashConvertibleCoins}")
      }

      val updatedWallet = currentWallet.copy(
        cashConvertibleCoins = currentWallet.cashConvertibleCoins - requestedCoins,
        pendingWithdrawalCoins = currentWallet.pendingWithdrawalCoins + requestedCoins,
        pendingWithdrawalPaisa = currentWallet.pendingWithdrawalPaisa + amountInPaise
      )

      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(requestRef, withdrawal)
      firestoreTx.set(txRef, tx)
    }.await()

    requestRef.id
  }

  // ---------------- ADMIN WITHDRAWAL ACTIONS ----------------

  /**
   * Admin approves, marks paid with UTR, or rejects (with refund) a withdrawal request.
   */
  suspend fun processWithdrawalReview(
    requestId: String,
    targetStatus: String, // "APPROVED", "PAID", "REJECTED"
    paymentReference: String = "",
    rejectionReason: String = ""
  ): Result<Unit> = runCatching {
    if (!isCurrentUserAdmin()) {
      throw IllegalAccessException("Admin authorization required.")
    }

    val adminUid = requireUserId()
    val requestRef = db.collection("withdrawalRequests").document(requestId)

    db.runTransaction { firestoreTx ->
      val reqSnap = firestoreTx.get(requestRef)
      if (!reqSnap.exists()) throw IllegalArgumentException("Withdrawal request not found.")
      val request = reqSnap.toObject(WithdrawalRequest::class.java)
        ?: throw IllegalStateException("Failed to parse request.")

      val walletRef = db.collection("wallets").document(request.userId)
      val walletSnap = firestoreTx.get(walletRef)
      val wallet = if (walletSnap.exists()) walletSnap.toObject(UserWallet::class.java) else null

      val now = com.google.firebase.Timestamp.now()

      when (targetStatus) {
        "APPROVED" -> {
          firestoreTx.update(
            requestRef,
            mapOf(
              "status" to "APPROVED",
              "reviewedAt" to now,
              "reviewedBy" to adminUid,
              "adminNotes" to "Approved for bank payout queue."
            )
          )
        }

        "PAID" -> {
          if (paymentReference.isBlank()) {
            throw IllegalArgumentException("Payment reference / UTR ID is mandatory to confirm payout.")
          }

          firestoreTx.update(
            requestRef,
            mapOf(
              "status" to "PAID",
              "paymentReferenceId" to paymentReference.trim(),
              "reviewedAt" to now,
              "paidAt" to now,
              "reviewedBy" to adminUid,
              "adminNotes" to "Paid via UPI (UTR: ${paymentReference.trim()})"
            )
          )

          // Update wallet balances: reduce pending, increment paid
          if (wallet != null) {
            val updatedWallet = wallet.copy(
              pendingWithdrawalCoins = (wallet.pendingWithdrawalCoins - request.requestedCoins).coerceAtLeast(0L),
              pendingWithdrawalPaisa = (wallet.pendingWithdrawalPaisa - request.amountInPaise).coerceAtLeast(0L),
              paidWithdrawalPaisa = wallet.paidWithdrawalPaisa + request.amountInPaise
            )
            firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
          }

          // Ledger record for payout completion
          val txRef = db.collection("coinTransactions").document()
          val payoutTx = CoinTransaction(
            transactionId = txRef.id,
            userId = request.userId,
            type = "WITHDRAWAL_PAYOUT",
            source = "WITHDRAWAL",
            coinAmount = request.requestedCoins,
            paisaEquivalent = request.amountInPaise,
            isCashConvertible = true,
            description = "UPI Payout Complete! UTR: ${paymentReference.trim()}",
            idempotencyKey = "wd_payout_$requestId",
            status = "COMPLETED"
          )
          firestoreTx.set(txRef, payoutTx)
        }

        "REJECTED" -> {
          val reason = rejectionReason.ifBlank { "Rejected by administration." }
          firestoreTx.update(
            requestRef,
            mapOf(
              "status" to "REJECTED",
              "rejectionReason" to reason,
              "reviewedAt" to now,
              "reviewedBy" to adminUid,
              "adminNotes" to reason
            )
          )

          // Refund coins back to cashConvertibleCoins
          if (wallet != null) {
            val updatedWallet = wallet.copy(
              cashConvertibleCoins = wallet.cashConvertibleCoins + request.requestedCoins,
              pendingWithdrawalCoins = (wallet.pendingWithdrawalCoins - request.requestedCoins).coerceAtLeast(0L),
              pendingWithdrawalPaisa = (wallet.pendingWithdrawalPaisa - request.amountInPaise).coerceAtLeast(0L),
              rejectedWithdrawalPaisa = wallet.rejectedWithdrawalPaisa + request.amountInPaise
            )
            firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
          }

          // Ledger record for refund
          val txRef = db.collection("coinTransactions").document()
          val refundTx = CoinTransaction(
            transactionId = txRef.id,
            userId = request.userId,
            type = "WITHDRAWAL_REFUND",
            source = "WITHDRAWAL",
            coinAmount = request.requestedCoins,
            paisaEquivalent = request.amountInPaise,
            isCashConvertible = true,
            description = "Refund: Withdrawal rejected (${reason.take(40)})",
            idempotencyKey = "wd_refund_$requestId",
            status = "COMPLETED"
          )
          firestoreTx.set(txRef, refundTx)
        }
      }
    }.await()
  }

  // ---------------- DAILY CHECK-IN REWARD (20 COINS) ----------------

  fun observeTodayCheckin(userId: String): Flow<DailyCheckinRecord?> = flow {
    val todayStr = todayDateFormat.format(Date())
    val checkinId = "${userId}_$todayStr"
    emitAll(
      db.collection("dailyCheckins").document(checkinId)
        .snapshots()
        .map { snap -> if (snap.exists()) snap.toObject(DailyCheckinRecord::class.java) else null }
        .catch { emit(null) }
    )
  }

  suspend fun claimDailyCheckin(userId: String): Result<DailyCheckinRecord> = runCatching {
    val todayStr = todayDateFormat.format(Date())
    val checkinId = "${userId}_$todayStr"
    val checkinRef = db.collection("dailyCheckins").document(checkinId)

    // Check if already claimed today
    val existingSnap = checkinRef.get().await()
    if (existingSnap.exists()) {
      throw IllegalStateException("Daily check-in reward already claimed for today!")
    }

    // Determine streak
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = todayDateFormat.format(cal.time)
    val yesterdayRef = db.collection("dailyCheckins").document("${userId}_$yesterdayStr")
    val yesterdaySnap = yesterdayRef.get().await()
    val streakDays = if (yesterdaySnap.exists()) {
      (yesterdaySnap.getLong("streakDays")?.toInt() ?: 0) + 1
    } else {
      1
    }

    val config = getEarningConfig()
    val rewardCoins = 20L
    val paisa = config.calculateInrPaise(rewardCoins)

    val record = DailyCheckinRecord(
      checkinId = checkinId,
      userId = userId,
      dateString = todayStr,
      rewardCoins = rewardCoins,
      streakDays = streakDays
    )

    val txRef = db.collection("coinTransactions").document()
    val tx = CoinTransaction(
      transactionId = txRef.id,
      userId = userId,
      type = "DAILY_CHECKIN",
      source = "DAILY_CHECKIN",
      coinAmount = rewardCoins,
      paisaEquivalent = paisa,
      isCashConvertible = true,
      description = "Daily Check-in Reward (Streak: $streakDays days)",
      idempotencyKey = "checkin_$checkinId",
      status = "COMPLETED"
    )

    val walletRef = db.collection("wallets").document(userId)

    db.runTransaction { firestoreTx ->
      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val newToday = if (currentWallet.lastEarningDate == todayStr) {
        currentWallet.todayEarnedCoins + rewardCoins
      } else {
        rewardCoins
      }

      val updatedWallet = currentWallet.copy(
        cashConvertibleCoins = currentWallet.cashConvertibleCoins + rewardCoins,
        totalEarnedLifetimeCoins = currentWallet.totalEarnedLifetimeCoins + rewardCoins,
        todayEarnedCoins = newToday,
        lastEarningDate = todayStr
      )

      firestoreTx.set(checkinRef, record)
      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    record
  }

  // ---------------- DAILY CHALLENGE REWARD (5 COINS/QUIZ, MAX 25) ----------------

  fun observeTodayDailyChallenge(userId: String): Flow<DailyChallengeRecord?> = flow {
    val todayStr = todayDateFormat.format(Date())
    val challengeId = "${userId}_$todayStr"
    emitAll(
      db.collection("dailyChallenges").document(challengeId)
        .snapshots()
        .map { snap -> if (snap.exists()) snap.toObject(DailyChallengeRecord::class.java) else null }
        .catch { emit(null) }
    )
  }

  suspend fun recordQuizCompletionDailyChallenge(userId: String, quizId: String): Result<Long> = runCatching {
    val todayStr = todayDateFormat.format(Date())
    val challengeId = "${userId}_$todayStr"
    val challengeRef = db.collection("dailyChallenges").document(challengeId)
    val walletRef = db.collection("wallets").document(userId)
    val config = getEarningConfig()

    val rewardPerQuiz = 5L
    val maxChallengeCoins = 25L

    var awardedCoins = 0L

    db.runTransaction { firestoreTx ->
      val challengeSnap = firestoreTx.get(challengeRef)
      val currentRecord = if (challengeSnap.exists()) {
        challengeSnap.toObject(DailyChallengeRecord::class.java) ?: DailyChallengeRecord(challengeId = challengeId, userId = userId, dateString = todayStr)
      } else {
        DailyChallengeRecord(challengeId = challengeId, userId = userId, dateString = todayStr)
      }

      // Check if already completed this specific quiz today
      if (currentRecord.completedQuizIds.contains(quizId)) {
        return@runTransaction
      }

      // Max 5 challenge quizzes per day
      if (currentRecord.completedCount >= 5 || currentRecord.totalCoinsEarned >= maxChallengeCoins) {
        return@runTransaction
      }

      awardedCoins = rewardPerQuiz
      val newCompletedIds = currentRecord.completedQuizIds + quizId
      val newCompletedCount = currentRecord.completedCount + 1
      val newTotalCoins = currentRecord.totalCoinsEarned + awardedCoins

      val updatedRecord = currentRecord.copy(
        completedQuizIds = newCompletedIds,
        completedCount = newCompletedCount,
        totalCoinsEarned = newTotalCoins
      )

      val txRef = db.collection("coinTransactions").document()
      val paisa = config.calculateInrPaise(awardedCoins)
      val tx = CoinTransaction(
        transactionId = txRef.id,
        userId = userId,
        type = "DAILY_CHALLENGE",
        source = "DAILY_CHALLENGE",
        coinAmount = awardedCoins,
        paisaEquivalent = paisa,
        isCashConvertible = true,
        description = "Daily Quiz Challenge ($newCompletedCount/5 Completed)",
        idempotencyKey = "challenge_${challengeId}_${quizId}",
        status = "COMPLETED"
      )

      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val newToday = if (currentWallet.lastEarningDate == todayStr) {
        currentWallet.todayEarnedCoins + awardedCoins
      } else {
        awardedCoins
      }

      val updatedWallet = currentWallet.copy(
        cashConvertibleCoins = currentWallet.cashConvertibleCoins + awardedCoins,
        totalEarnedLifetimeCoins = currentWallet.totalEarnedLifetimeCoins + awardedCoins,
        todayEarnedCoins = newToday,
        lastEarningDate = todayStr
      )

      firestoreTx.set(challengeRef, updatedRecord, SetOptions.merge())
      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    awardedCoins
  }

  // ---------------- CORRECT ANSWER REWARD (2 COINS) ----------------

  suspend fun claimCorrectAnswerReward(userId: String, questionId: String, quizId: String): Result<Long> = runCatching {
    if (questionId.isBlank()) return@runCatching 0L
    val rewardDocId = "${userId}_${questionId}"
    val rewardRef = db.collection("answerRewards").document(rewardDocId)
    val walletRef = db.collection("wallets").document(userId)
    val config = getEarningConfig()

    var awardedCoins = 0L

    db.runTransaction { firestoreTx ->
      val snap = firestoreTx.get(rewardRef)
      if (snap.exists()) {
        return@runTransaction // Already rewarded for this question
      }

      awardedCoins = 2L
      val record = AnswerRewardRecord(
        rewardId = rewardDocId,
        userId = userId,
        questionId = questionId,
        quizId = quizId,
        rewardCoins = awardedCoins
      )

      val txRef = db.collection("coinTransactions").document()
      val paisa = config.calculateInrPaise(awardedCoins)
      val tx = CoinTransaction(
        transactionId = txRef.id,
        userId = userId,
        type = "CORRECT_ANSWER",
        source = "CORRECT_ANSWER",
        coinAmount = awardedCoins,
        paisaEquivalent = paisa,
        isCashConvertible = true,
        description = "Correct Answer Bonus",
        idempotencyKey = "ans_reward_$rewardDocId",
        status = "COMPLETED"
      )

      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val todayStr = todayDateFormat.format(Date())
      val newToday = if (currentWallet.lastEarningDate == todayStr) {
        currentWallet.todayEarnedCoins + awardedCoins
      } else {
        awardedCoins
      }

      val updatedWallet = currentWallet.copy(
        cashConvertibleCoins = currentWallet.cashConvertibleCoins + awardedCoins,
        totalEarnedLifetimeCoins = currentWallet.totalEarnedLifetimeCoins + awardedCoins,
        todayEarnedCoins = newToday,
        lastEarningDate = todayStr
      )

      firestoreTx.set(rewardRef, record)
      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    awardedCoins
  }

  // ---------------- COMMUNITY POST REWARD (5 COINS) ----------------

  suspend fun claimPostReward(userId: String, postId: String, postTitle: String, content: String): Result<Long> = runCatching {
    if (content.trim().length < 15) {
      return@runCatching 0L // Anti-spam: minimum length required
    }

    val postRewardRef = db.collection("postRewards").document(postId)
    val walletRef = db.collection("wallets").document(userId)
    val config = getEarningConfig()

    var awardedCoins = 0L

    db.runTransaction { firestoreTx ->
      val snap = firestoreTx.get(postRewardRef)
      if (snap.exists()) {
        return@runTransaction // Already rewarded
      }

      awardedCoins = 5L
      val record = PostRewardRecord(
        postId = postId,
        userId = userId,
        postTitle = postTitle,
        rewardCoins = awardedCoins,
        contentSnippet = content.take(60)
      )

      val txRef = db.collection("coinTransactions").document()
      val paisa = config.calculateInrPaise(awardedCoins)
      val tx = CoinTransaction(
        transactionId = txRef.id,
        userId = userId,
        type = "POST_REWARD",
        source = "POST_REWARD",
        coinAmount = awardedCoins,
        paisaEquivalent = paisa,
        isCashConvertible = true,
        description = "Community Content Reward: ${postTitle.take(30)}",
        idempotencyKey = "post_reward_$postId",
        status = "COMPLETED"
      )

      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val todayStr = todayDateFormat.format(Date())
      val newToday = if (currentWallet.lastEarningDate == todayStr) {
        currentWallet.todayEarnedCoins + awardedCoins
      } else {
        awardedCoins
      }

      val updatedWallet = currentWallet.copy(
        cashConvertibleCoins = currentWallet.cashConvertibleCoins + awardedCoins,
        totalEarnedLifetimeCoins = currentWallet.totalEarnedLifetimeCoins + awardedCoins,
        todayEarnedCoins = newToday,
        lastEarningDate = todayStr
      )

      firestoreTx.set(postRewardRef, record)
      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    awardedCoins
  }

  // ---------------- REWARDED AD REWARD (30 COINS) ----------------

  suspend fun claimRewardedAdCoins(
    userId: String,
    adNetwork: String, // "ADMOB" or "INMOBI"
    idempotencyKey: String
  ): Result<Long> = runCatching {
    val claimRef = db.collection("rewardedAdClaims").document(idempotencyKey)
    val walletRef = db.collection("wallets").document(userId)
    val config = getEarningConfig()

    val rewardAmount = 30L
    var isCashConvertible = true

    // Policy Check: If AdMob rewarded is strictly non-cash in this build, credit as appOnlyRewardPoints
    if (adNetwork == "ADMOB") {
      isCashConvertible = false
    } else if (adNetwork == "INMOBI" && !config.isInMobiCashConversionAllowed) {
      isCashConvertible = false
    }

    val record = RewardedAdClaimRecord(
      claimId = idempotencyKey,
      userId = userId,
      adNetwork = adNetwork,
      rewardCoins = rewardAmount,
      isCashConvertible = isCashConvertible
    )

    val txRef = db.collection("coinTransactions").document()
    val paisa = if (isCashConvertible) config.calculateInrPaise(rewardAmount) else 0L
    val tx = CoinTransaction(
      transactionId = txRef.id,
      userId = userId,
      type = "REWARDED_AD",
      source = adNetwork,
      coinAmount = rewardAmount,
      paisaEquivalent = paisa,
      isCashConvertible = isCashConvertible,
      description = if (isCashConvertible) "$adNetwork Sponsored Video Reward (+30 Coins)" else "$adNetwork Bonus Points (+30 In-App Points)",
      idempotencyKey = idempotencyKey,
      status = "COMPLETED"
    )

    db.runTransaction { firestoreTx ->
      val claimSnap = firestoreTx.get(claimRef)
      if (claimSnap.exists()) {
        return@runTransaction // Idempotency check: already rewarded
      }

      val walletSnap = firestoreTx.get(walletRef)
      val currentWallet = if (walletSnap.exists()) {
        walletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = userId)
      } else {
        UserWallet(userId = userId)
      }

      val todayStr = todayDateFormat.format(Date())
      val updatedWallet = if (isCashConvertible) {
        val newToday = if (currentWallet.lastEarningDate == todayStr) {
          currentWallet.todayEarnedCoins + rewardAmount
        } else {
          rewardAmount
        }
        currentWallet.copy(
          cashConvertibleCoins = currentWallet.cashConvertibleCoins + rewardAmount,
          totalEarnedLifetimeCoins = currentWallet.totalEarnedLifetimeCoins + rewardAmount,
          todayEarnedCoins = newToday,
          lastEarningDate = todayStr
        )
      } else {
        currentWallet.copy(
          appOnlyRewardPoints = currentWallet.appOnlyRewardPoints + rewardAmount
        )
      }

      firestoreTx.set(claimRef, record)
      firestoreTx.set(walletRef, updatedWallet, SetOptions.merge())
      firestoreTx.set(txRef, tx)
    }.await()

    rewardAmount
  }

  // ---------------- REFERRAL SYSTEM REWARD (500 COINS) ----------------

  suspend fun claimReferralReward(
    referrerCode: String,
    refereeUserId: String,
    refereeName: String
  ): Result<Long> = runCatching {
    val cleanCode = referrerCode.trim().uppercase()
    if (cleanCode.isBlank()) throw IllegalArgumentException("Please enter a valid referral code.")

    // 1. Find referrer user profile
    val usersQuery = db.collection("users")
      .whereEqualTo("referralCode", cleanCode)
      .limit(1)
      .get()
      .await()

    if (usersQuery.isEmpty) {
      throw IllegalArgumentException("No community member found with referral code \"$cleanCode\".")
    }

    val referrerDoc = usersQuery.documents.first()
    val referrerUid = referrerDoc.id
    if (referrerUid == refereeUserId) {
      throw IllegalArgumentException("Anti-Abuse Rule: You cannot use your own referral code!")
    }

    // 2. Check if referee has already claimed a referral bonus
    val existingClaim = db.collection("referrals").document(refereeUserId).get().await()
    if (existingClaim.exists()) {
      throw IllegalStateException("You have already claimed a referral bonus!")
    }

    val config = getEarningConfig()
    val referrerRewardCoins = 500L
    val refereeWelcomeCoins = 50L

    val refRecordRef = db.collection("referrals").document(refereeUserId)
    val referrerWalletRef = db.collection("wallets").document(referrerUid)
    val refereeWalletRef = db.collection("wallets").document(refereeUserId)

    val referrerTxRef = db.collection("coinTransactions").document()
    val refereeTxRef = db.collection("coinTransactions").document()

    val referrerPaisa = config.calculateInrPaise(referrerRewardCoins)
    val refereePaisa = config.calculateInrPaise(refereeWelcomeCoins)

    val referrerTx = CoinTransaction(
      transactionId = referrerTxRef.id,
      userId = referrerUid,
      type = "REFERRAL_REWARD",
      source = "REFERRAL_SYSTEM",
      coinAmount = referrerRewardCoins,
      paisaEquivalent = referrerPaisa,
      isCashConvertible = true,
      description = "Referral Bonus: Invited $refereeName",
      idempotencyKey = "ref_${referrerUid}_${refereeUserId}",
      status = "COMPLETED"
    )

    val refereeTx = CoinTransaction(
      transactionId = refereeTxRef.id,
      userId = refereeUserId,
      type = "REFERRAL_REWARD",
      source = "REFERRAL_SYSTEM",
      coinAmount = refereeWelcomeCoins,
      paisaEquivalent = refereePaisa,
      isCashConvertible = true,
      description = "Welcome Bonus for joining via code $cleanCode",
      idempotencyKey = "welcome_ref_${refereeUserId}",
      status = "COMPLETED"
    )

    db.runTransaction { firestoreTx ->
      // Referrer wallet
      val referrerWalletSnap = firestoreTx.get(referrerWalletRef)
      val currentReferrerWallet = if (referrerWalletSnap.exists()) {
        referrerWalletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = referrerUid)
      } else {
        UserWallet(userId = referrerUid)
      }

      val updatedReferrerWallet = currentReferrerWallet.copy(
        cashConvertibleCoins = currentReferrerWallet.cashConvertibleCoins + referrerRewardCoins,
        totalEarnedLifetimeCoins = currentReferrerWallet.totalEarnedLifetimeCoins + referrerRewardCoins
      )

      // Referee wallet
      val refereeWalletSnap = firestoreTx.get(refereeWalletRef)
      val currentRefereeWallet = if (refereeWalletSnap.exists()) {
        refereeWalletSnap.toObject(UserWallet::class.java) ?: UserWallet(userId = refereeUserId)
      } else {
        UserWallet(userId = refereeUserId)
      }

      val updatedRefereeWallet = currentRefereeWallet.copy(
        cashConvertibleCoins = currentRefereeWallet.cashConvertibleCoins + refereeWelcomeCoins,
        totalEarnedLifetimeCoins = currentRefereeWallet.totalEarnedLifetimeCoins + refereeWelcomeCoins
      )

      // Save referral record
      firestoreTx.set(
        refRecordRef,
        mapOf(
          "referralId" to refereeUserId,
          "referrerUserId" to referrerUid,
          "refereeUserId" to refereeUserId,
          "referralCode" to cleanCode,
          "referrerRewardCoins" to referrerRewardCoins,
          "refereeRewardCoins" to refereeWelcomeCoins,
          "claimedAt" to FieldValue.serverTimestamp()
        )
      )

      // Increment referrer user profile counter
      firestoreTx.update(
        referrerDoc.reference,
        mapOf(
          "referralCount" to FieldValue.increment(1),
          "referralPointsEarned" to FieldValue.increment(referrerRewardCoins)
        )
      )

      firestoreTx.set(referrerWalletRef, updatedReferrerWallet, SetOptions.merge())
      firestoreTx.set(refereeWalletRef, updatedRefereeWallet, SetOptions.merge())
      firestoreTx.set(referrerTxRef, referrerTx)
      firestoreTx.set(refereeTxRef, refereeTx)
    }.await()

    referrerRewardCoins
  }
}
