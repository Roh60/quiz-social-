package com.example.ui.navigation

import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.data.local.AppSettingsRepository
import com.example.data.local.DraftDao
import com.example.data.remote.QuizPollRepository
import com.example.ui.screens.create.CreateDiscussionScreen
import com.example.ui.screens.create.CreateHubScreen
import com.example.ui.screens.create.CreatePollScreen
import com.example.ui.screens.create.CreateQuizScreen
import com.example.ui.screens.discussion.DiscussionDetailScreen
import com.example.ui.screens.explore.ExploreScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.profile.BookmarksScreen
import com.example.ui.screens.profile.DraftsScreen
import com.example.ui.screens.profile.FollowersScreen
import com.example.ui.screens.profile.FollowingScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.quiz.QuizFeedScreen
import com.example.ui.screens.quiz.QuizPlayerScreen
import com.example.ui.screens.quiz.QuizResultScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.data.remote.WalletRepository
import com.example.ui.screens.wallet.WalletDashboardScreen
import com.example.ui.screens.wallet.EarnRewardsScreen
import com.example.ui.screens.wallet.WithdrawalScreen
import com.example.ui.screens.wallet.AdminWithdrawalScreen
import com.example.ui.screens.wallet.ReferralScreen

enum class BottomTab(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
) {
  HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "bottom_tab_home"),
  EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore, "bottom_tab_explore"),
  CREATE("Create", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, "bottom_tab_create"),
  NOTIFICATIONS("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications, "bottom_tab_notifications"),
  PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "bottom_tab_profile")
}

sealed class SubScreen {
  data class Search(val initialCategory: String? = null) : SubScreen()
  object CreateQuiz : SubScreen()
  object CreatePoll : SubScreen()
  object CreateDiscussion : SubScreen()
  object Drafts : SubScreen()
  data class QuizPlayer(val quizId: String) : SubScreen()
  data class QuizResult(
    val quizId: String,
    val score: Int,
    val total: Int,
    val timeTaken: Int,
    val userAnswers: Map<Int, List<Int>>
  ) : SubScreen()
  data class DiscussionDetail(val postId: String) : SubScreen()
  data class Followers(val userId: String) : SubScreen()
  data class Following(val userId: String) : SubScreen()
  object Bookmarks : SubScreen()
  object Settings : SubScreen()
  object QuizFeed : SubScreen()
  object Wallet : SubScreen()
  object EarnRewards : SubScreen()
  object Withdrawal : SubScreen()
  object AdminWithdrawals : SubScreen()
  object Referral : SubScreen()
}

@Composable
fun AppNavigation(
  repository: QuizPollRepository,
  settingsRepo: AppSettingsRepository,
  draftDao: DraftDao?,
  currentUserId: String,
  onSignedOut: () -> Unit,
  walletRepo: WalletRepository = remember { WalletRepository() },
  modifier: Modifier = Modifier
) {
  var currentTab by remember { mutableStateOf(BottomTab.HOME) }
  var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }

  val rewardContext = androidx.compose.ui.platform.LocalContext.current
  val rewardScope = androidx.compose.runtime.rememberCoroutineScope()
  val rewards = remember {
    com.example.data.remote.RewardCoordinator(
      com.google.firebase.firestore.FirebaseFirestore.getInstance(
        rewardContext.applicationContext.getString(com.example.R.string.firestore_database_id)
      )
    )
  }
  val giveReward: (suspend () -> String?) -> Unit = { block ->
    rewardScope.launch {
      val message = try {
        block()
      } catch (e: Exception) {
        "Could not add coins: ${e.message}"
      }
      if (message != null) {
        android.widget.Toast.makeText(rewardContext, message, android.widget.Toast.LENGTH_LONG).show()
      }
    }
  }

  Scaffold(
    bottomBar = {
      if (currentSubScreen == null) {
        androidx.compose.foundation.layout.Column {
        com.example.ui.components.BannerAdView()
        NavigationBar {
          BottomTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
              selected = isSelected,
              onClick = { currentTab = tab },
              icon = {
                Icon(
                  imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = tab.title
                )
              },
              label = { Text(tab.title) },
              modifier = Modifier.testTag(tab.testTag)
            )
          }
        }
        }
      }
    }
  ) { innerPadding ->
    val contentModifier = modifier
      .fillMaxSize()
      .padding(innerPadding)

    if (currentSubScreen != null) {
      BackHandler {
        currentSubScreen = null
      }
      when (val sub = currentSubScreen!!) {
        is SubScreen.Search -> {
          SearchScreen(
            initialCategory = sub.initialCategory,
            repository = repository,
            currentUserId = currentUserId,
            onNavigateBack = { currentSubScreen = null },
            onNavigateToPostDetail = { postId -> currentSubScreen = SubScreen.DiscussionDetail(postId) },
            onNavigateToQuizPlayer = { quizId -> currentSubScreen = SubScreen.QuizPlayer(quizId) }
          )
        }
        is SubScreen.CreateQuiz -> {
          CreateQuizScreen(
            repository = repository,
            draftDao = draftDao,
            onNavigateBack = { currentSubScreen = null },
            onQuizPublished = { quizId ->
              giveReward { rewards.rewardCreatedContent(quizId, "quiz") }
              currentSubScreen = SubScreen.QuizPlayer(quizId)
            }
          )
        }
        is SubScreen.CreatePoll -> {
          CreatePollScreen(
            repository = repository,
            draftDao = draftDao,
            onNavigateBack = { currentSubScreen = null },
            onPollPublished = { pollId ->
              giveReward { rewards.rewardCreatedContent(pollId, "poll") }
              currentSubScreen = null
              currentTab = BottomTab.HOME
            }
          )
        }
        is SubScreen.CreateDiscussion -> {
          CreateDiscussionScreen(
            repository = repository,
            onNavigateBack = { currentSubScreen = null },
            onDiscussionPublished = { postId ->
              giveReward { rewards.rewardDiscussion(postId) }
              currentSubScreen = SubScreen.DiscussionDetail(postId)
            }
          )
        }
        is SubScreen.Drafts -> {
          DraftsScreen(
            draftDao = draftDao,
            onNavigateBack = { currentSubScreen = null },
            onSelectQuizDraft = { currentSubScreen = SubScreen.CreateQuiz },
            onSelectPollDraft = { currentSubScreen = SubScreen.CreatePoll }
          )
        }
        is SubScreen.QuizPlayer -> {
          QuizPlayerScreen(
            quizId = sub.quizId,
            repository = repository,
            onNavigateBack = { currentSubScreen = null },
            onQuizCompleted = { score, total, timeTaken, answers ->
              giveReward { rewards.rewardQuiz(sub.quizId, score) }
              currentSubScreen = SubScreen.QuizResult(sub.quizId, score, total, timeTaken, answers)
            }
          )
        }
        is SubScreen.QuizResult -> {
          QuizResultScreen(
            quizId = sub.quizId,
            score = sub.score,
            totalQuestions = sub.total,
            timeTakenSeconds = sub.timeTaken,
            userAnswers = sub.userAnswers,
            repository = repository,
            onRetryQuiz = { currentSubScreen = SubScreen.QuizPlayer(sub.quizId) },
            onReturnHome = { currentSubScreen = null; currentTab = BottomTab.HOME }
          )
        }
        is SubScreen.DiscussionDetail -> {
          DiscussionDetailScreen(
            postId = sub.postId,
            repository = repository,
            currentUserId = currentUserId,
            onNavigateBack = { currentSubScreen = null },
            onNavigateToQuizPlayer = { quizId -> currentSubScreen = SubScreen.QuizPlayer(quizId) }
          )
        }
        is SubScreen.Followers -> {
          FollowersScreen(
            userId = sub.userId,
            repository = repository,
            onNavigateBack = { currentSubScreen = null }
          )
        }
        is SubScreen.Following -> {
          FollowingScreen(
            userId = sub.userId,
            repository = repository,
            onNavigateBack = { currentSubScreen = null }
          )
        }
        is SubScreen.Bookmarks -> {
          BookmarksScreen(
            repository = repository,
            onNavigateBack = { currentSubScreen = null },
            onNavigateToPost = { postId -> currentSubScreen = SubScreen.DiscussionDetail(postId) }
          )
        }
        is SubScreen.Settings -> {
          SettingsScreen(
            settingsRepo = settingsRepo,
            quizPollRepo = repository,
            onNavigateBack = { currentSubScreen = null },
            onSignedOut = onSignedOut
          )
        }
        is SubScreen.QuizFeed -> {
          QuizFeedScreen(
            repository = repository,
            onNavigateBack = { currentSubScreen = null },
            onTakeQuiz = { quizId -> currentSubScreen = SubScreen.QuizPlayer(quizId) }
          )
        }
        is SubScreen.Wallet -> {
          WalletDashboardScreen(
            walletRepository = walletRepo,
            currentUserId = currentUserId,
            onNavigateBack = { currentSubScreen = null },
            onNavigateToEarn = { currentSubScreen = SubScreen.EarnRewards },
            onNavigateToWithdraw = { currentSubScreen = SubScreen.Withdrawal },
            onNavigateToAdmin = { currentSubScreen = SubScreen.AdminWithdrawals }
          )
        }
        is SubScreen.EarnRewards -> {
          EarnRewardsScreen(
            walletRepository = walletRepo,
            currentUserId = currentUserId,
            onNavigateBack = { currentSubScreen = SubScreen.Wallet },
            onNavigateToQuizzes = { currentSubScreen = SubScreen.QuizFeed },
            onNavigateToPolls = { currentSubScreen = null; currentTab = BottomTab.HOME },
            onNavigateToReferral = { currentSubScreen = SubScreen.Referral }
          )
        }
        is SubScreen.Withdrawal -> {
          WithdrawalScreen(
            walletRepository = walletRepo,
            currentUserId = currentUserId,
            onNavigateBack = { currentSubScreen = SubScreen.Wallet }
          )
        }
        is SubScreen.AdminWithdrawals -> {
          AdminWithdrawalScreen(
            walletRepository = walletRepo,
            currentUserId = currentUserId,
            onNavigateBack = { currentSubScreen = SubScreen.Wallet }
          )
        }
        is SubScreen.Referral -> {
          ReferralScreen(
            walletRepository = walletRepo,
            quizPollRepository = repository,
            currentUserId = currentUserId,
            onNavigateBack = { currentSubScreen = SubScreen.Wallet }
          )
        }
      }
    } else {
      when (currentTab) {
        BottomTab.HOME -> {
          HomeScreen(
            repository = repository,
            currentUserId = currentUserId,
            onNavigateToSearch = { currentSubScreen = SubScreen.Search() },
            onNavigateToNotifications = { currentTab = BottomTab.NOTIFICATIONS },
            onNavigateToPostDetail = { postId -> currentSubScreen = SubScreen.DiscussionDetail(postId) },
            onNavigateToQuizPlayer = { quizId -> currentSubScreen = SubScreen.QuizPlayer(quizId) },
            onNavigateToWallet = { currentSubScreen = SubScreen.Wallet }
          )
        }
        BottomTab.EXPLORE -> {
          ExploreScreen(
            repository = repository,
            currentUserId = currentUserId,
            onNavigateToSearch = { cat -> currentSubScreen = SubScreen.Search(cat) },
            onNavigateToPostDetail = { postId -> currentSubScreen = SubScreen.DiscussionDetail(postId) },
            onNavigateToQuizPlayer = { quizId -> currentSubScreen = SubScreen.QuizPlayer(quizId) }
          )
        }
        BottomTab.CREATE -> {
          CreateHubScreen(
            onNavigateToCreateQuiz = { currentSubScreen = SubScreen.CreateQuiz },
            onNavigateToCreatePoll = { currentSubScreen = SubScreen.CreatePoll },
            onNavigateToCreateDiscussion = { currentSubScreen = SubScreen.CreateDiscussion },
            onNavigateToDrafts = { currentSubScreen = SubScreen.Drafts }
          )
        }
        BottomTab.NOTIFICATIONS -> {
          NotificationsScreen(
            repository = repository,
            onNavigateBack = { currentTab = BottomTab.HOME },
            onNavigateToPost = { postId -> currentSubScreen = SubScreen.DiscussionDetail(postId) }
          )
        }
        BottomTab.PROFILE -> {
          ProfileScreen(
            repository = repository,
            currentUserId = currentUserId,
            onNavigateToSettings = { currentSubScreen = SubScreen.Settings },
            onNavigateToFollowers = { uid -> currentSubScreen = SubScreen.Followers(uid) },
            onNavigateToFollowing = { uid -> currentSubScreen = SubScreen.Following(uid) },
            onNavigateToPostDetail = { postId -> currentSubScreen = SubScreen.DiscussionDetail(postId) },
            onNavigateToQuizPlayer = { quizId -> currentSubScreen = SubScreen.QuizPlayer(quizId) },
            onNavigateToWallet = { currentSubScreen = SubScreen.Wallet }
          )
        }
      }
    }
  }
}
