package com.example

import com.example.data.ads.AdConstants
import com.example.data.ads.InMobiManager
import com.example.data.model.EarningConfig
import com.example.data.model.UserWallet
import com.example.data.model.WithdrawalRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit and compliance tests for QuizPoll Community monetization, integer accounting,
 * coin-to-INR conversions, and withdrawal rules.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MonetizationAndWalletTest {

  @Test
  fun testExactIntegerCoinConversionRate() {
    val config = EarningConfig()

    // Base conversion: 1000 coins = ₹5.00 INR (500 paise)
    assertEquals(500L, config.calculateInrPaise(1000L))
    assertEquals(5.0, config.calculateInrRupees(1000L), 0.001)

    // User prompt examples:
    // 5000 coins = ₹25
    assertEquals(2500L, config.calculateInrPaise(5000L))
    assertEquals(25.0, config.calculateInrRupees(5000L), 0.001)

    // 10000 coins = ₹50
    assertEquals(5000L, config.calculateInrPaise(10000L))
    assertEquals(50.0, config.calculateInrRupees(10000L), 0.001)

    // 20000 coins = ₹100
    assertEquals(10000L, config.calculateInrPaise(20000L))
    assertEquals(100.0, config.calculateInrRupees(20000L), 0.001)

    // 100000 coins = ₹500
    assertEquals(50000L, config.calculateInrPaise(100000L))
    assertEquals(500.0, config.calculateInrRupees(100000L), 0.001)
  }

  @Test
  fun testStrictWalletBalanceSegregation() {
    val wallet = UserWallet(
      userId = "user_test_123",
      cashConvertibleCoins = 10000L,
      appOnlyRewardPoints = 500L, // AdMob points
      pendingWithdrawalCoins = 5000L,
      pendingWithdrawalPaisa = 2500L,
      paidWithdrawalPaisa = 10000L
    )

    // Cash value must ONLY calculate from cashConvertibleCoins, NEVER mixing appOnlyRewardPoints
    assertEquals(5000L, wallet.inrValuePaise)
    assertEquals(50.0, wallet.inrValueDouble, 0.001)
    assertEquals("₹50.00", wallet.getFormattedInr())

    // Pending withdrawal check: 2500 paise = ₹25.00
    assertEquals("₹25.00", wallet.getFormattedPendingInr())

    // Paid withdrawal check: 10000 paise = ₹100.00
    assertEquals("₹100.00", wallet.getFormattedPaidInr())

    // Combined total coins for display
    assertEquals(10500L, wallet.totalCombinedDisplayCoins)
  }

  @Test
  fun testUpiIdValidationSyntax() {
    val upiRegex = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9]{2,64}$")

    // Valid UPI handles
    assertTrue(upiRegex.matches("rohit@oksbi"))
    assertTrue(upiRegex.matches("9876543210@paytm"))
    assertTrue(upiRegex.matches("john.doe-1@hdfcbank"))
    assertTrue(upiRegex.matches("user_name@apl"))

    // Invalid UPI handles
    assertFalse(upiRegex.matches("invalid_no_at_symbol"))
    assertFalse(upiRegex.matches("@onlyat"))
    assertFalse(upiRegex.matches("space in@bank"))
    assertFalse(upiRegex.matches("user@"))
  }

  @Test
  fun testAdMobPolicyCompliance() {
    // AdMob rewarded ad points are non-transferable app-only bonus points
    assertEquals(100L, AdConstants.ADMOB_REWARD_QUIZ_POINTS)

    val wallet = UserWallet(
      userId = "user_admob",
      cashConvertibleCoins = 0L,
      appOnlyRewardPoints = AdConstants.ADMOB_REWARD_QUIZ_POINTS
    )

    // AdMob points MUST NOT increase cash value
    assertEquals(0L, wallet.inrValuePaise)
    assertEquals(0.0, wallet.inrValueDouble, 0.001)
    assertEquals(100L, wallet.appOnlyRewardPoints)
  }

  @Test
  fun testInMobiCashConversionDisabledByDefault() {
    val config = EarningConfig()
    // Explicit compliance requirement: Cash-convertible InMobi rewards must be disabled by default
    assertFalse(config.isInMobiCashConversionAllowed)

    InMobiManager.setCashConvertibleRewardAllowed(false)
    assertFalse(InMobiManager.isCashConvertibleRewardAllowed())
  }

  @Test
  fun testWithdrawalRequestFormatting() {
    val req = WithdrawalRequest(
      requestId = "req_12345",
      userId = "u1",
      upiId = "test@upi",
      requestedCoins = 5000L,
      amountInPaise = 2500L,
      amountInRupees = 25.0,
      status = "PENDING"
    )

    assertEquals("₹25.00", req.formattedRupees)
    assertTrue(req.isActionableByAdmin)
  }

  @Test
  fun testRewardRulesAmounts() {
    assertEquals(30L, AdConstants.REWARDED_AD_COINS)
    assertEquals(5L, AdConstants.COMMUNITY_POST_COINS)
    assertEquals(5L, AdConstants.DAILY_CHALLENGE_PER_QUIZ_COINS)
    assertEquals(25L, AdConstants.DAILY_CHALLENGE_MAX_COINS)
    assertEquals(2L, AdConstants.CORRECT_ANSWER_COINS)
    assertEquals(20L, AdConstants.DAILY_CHECKIN_COINS)
    assertEquals(500L, AdConstants.REFERRAL_REWARD_COINS)
  }

  @Test
  fun testUpiMaskingForPrivacy() {
    val req1 = WithdrawalRequest(upiId = "rohit@oksbi")
    assertEquals("ro***t@oksbi", req1.maskedUpiId)

    val req2 = WithdrawalRequest(upiId = "john.doe@okhdfcbank")
    assertEquals("jo***e@okhdfcbank", req2.maskedUpiId)

    val req3 = WithdrawalRequest(upiId = "ab@upi")
    assertEquals("***@upi", req3.maskedUpiId)
  }

  @Test
  fun testWalletAccountingConvenienceGetters() {
    val wallet = UserWallet(
      userId = "u_ledger",
      cashConvertibleCoins = 8000L,
      pendingWithdrawalCoins = 5000L,
      pendingWithdrawalPaisa = 2500L,
      paidWithdrawalPaisa = 10000L,
      totalEarnedLifetimeCoins = 23000L
    )

    assertEquals(8000L, wallet.availableCoins)
    assertEquals(5000L, wallet.pendingCoins)
    assertEquals(13000L, wallet.totalCoinBalance)
    assertEquals(23000L, wallet.totalEarnedCoins)
    assertEquals(100.0, wallet.totalWithdrawnInr, 0.001)
    assertEquals(20000L, wallet.redeemedCoins)
  }
}
