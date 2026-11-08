package com.example.data.model

enum class BadgeCategory {
  CREATION,
  DISCUSSION,
  MILESTONE
}

data class UserBadge(
  val id: String,
  val name: String,
  val description: String,
  val category: BadgeCategory,
  val iconName: String,
  val currentProgress: Int,
  val targetRequirement: Int,
  val isUnlocked: Boolean,
  val progressPercent: Float = if (targetRequirement > 0) (currentProgress.toFloat() / targetRequirement).coerceIn(0f, 1f) else 1f
)

data class ActivityStats(
  val quizzesCreated: Int,
  val pollsCreated: Int,
  val commentsContributed: Int,
  val totalPoints: Int,
  val levelName: String,
  val levelNumber: Int,
  val pointsToNextLevel: Int,
  val levelProgress: Float,
  val badges: List<UserBadge>
)

object ActivityPointsCalculator {
  const val POINTS_PER_QUIZ = 25
  const val POINTS_PER_POLL = 20
  const val POINTS_PER_COMMENT = 10

  fun calculatePoints(quizzes: Int, polls: Int, comments: Int): Int {
    return (quizzes * POINTS_PER_QUIZ) + (polls * POINTS_PER_POLL) + (comments * POINTS_PER_COMMENT)
  }

  fun calculateLevel(points: Int): Pair<Int, String> {
    return when {
      points >= 600 -> 5 to "Community Master"
      points >= 300 -> 4 to "Knowledge Builder"
      points >= 150 -> 3 to "Community Contributor"
      points >= 50 -> 2 to "Curious Thinker"
      else -> 1 to "Novice Explorer"
    }
  }

  fun calculateNextLevelThreshold(points: Int): Int {
    return when {
      points >= 600 -> 1000
      points >= 300 -> 600
      points >= 150 -> 300
      points >= 50 -> 150
      else -> 50
    }
  }

  fun generateBadges(quizzesCreated: Int, pollsCreated: Int, commentsContributed: Int): List<UserBadge> {
    val totalCreations = quizzesCreated + pollsCreated
    val totalPoints = calculatePoints(quizzesCreated, pollsCreated, commentsContributed)

    return listOf(
      UserBadge(
        id = "first_spark",
        name = "First Spark",
        description = "Create your first quiz or community poll",
        category = BadgeCategory.CREATION,
        iconName = "Lightbulb",
        currentProgress = totalCreations,
        targetRequirement = 1,
        isUnlocked = totalCreations >= 1
      ),
      UserBadge(
        id = "quiz_architect",
        name = "Quiz Architect",
        description = "Create 3 engaging community quizzes",
        category = BadgeCategory.CREATION,
        iconName = "Psychology",
        currentProgress = quizzesCreated,
        targetRequirement = 3,
        isUnlocked = quizzesCreated >= 3
      ),
      UserBadge(
        id = "master_pollster",
        name = "Master Pollster",
        description = "Publish 3 community polls to spark discussions",
        category = BadgeCategory.CREATION,
        iconName = "Poll",
        currentProgress = pollsCreated,
        targetRequirement = 3,
        isUnlocked = pollsCreated >= 3
      ),
      UserBadge(
        id = "conversation_starter",
        name = "Conversation Starter",
        description = "Contribute 3 thoughtful comments or answers",
        category = BadgeCategory.DISCUSSION,
        iconName = "Chat",
        currentProgress = commentsContributed,
        targetRequirement = 3,
        isUnlocked = commentsContributed >= 3
      ),
      UserBadge(
        id = "discussion_guru",
        name = "Discussion Guru",
        description = "Contribute 10 comments across quizzes and discussions",
        category = BadgeCategory.DISCUSSION,
        iconName = "Star",
        currentProgress = commentsContributed,
        targetRequirement = 10,
        isUnlocked = commentsContributed >= 10
      ),
      UserBadge(
        id = "prolific_author",
        name = "Prolific Author",
        description = "Create at least 5 quizzes and polls combined",
        category = BadgeCategory.CREATION,
        iconName = "MilitaryTech",
        currentProgress = totalCreations,
        targetRequirement = 5,
        isUnlocked = totalCreations >= 5
      ),
      UserBadge(
        id = "community_legend",
        name = "Community Legend",
        description = "Accumulate 200 activity points through creation and discussion",
        category = BadgeCategory.MILESTONE,
        iconName = "EmojiEvents",
        currentProgress = totalPoints,
        targetRequirement = 200,
        isUnlocked = totalPoints >= 200
      )
    )
  }

  fun computeStats(quizzesCreated: Int, pollsCreated: Int, commentsContributed: Int): ActivityStats {
    val totalPoints = calculatePoints(quizzesCreated, pollsCreated, commentsContributed)
    val (levelNumber, levelName) = calculateLevel(totalPoints)
    val nextThreshold = calculateNextLevelThreshold(totalPoints)
    val prevThreshold = when (levelNumber) {
      5 -> 600
      4 -> 300
      3 -> 150
      2 -> 50
      else -> 0
    }
    val levelSpan = (nextThreshold - prevThreshold).coerceAtLeast(1)
    val progressInLevel = (totalPoints - prevThreshold).coerceAtLeast(0)
    val progressPercent = (progressInLevel.toFloat() / levelSpan).coerceIn(0f, 1f)

    return ActivityStats(
      quizzesCreated = quizzesCreated,
      pollsCreated = pollsCreated,
      commentsContributed = commentsContributed,
      totalPoints = totalPoints,
      levelName = levelName,
      levelNumber = levelNumber,
      pointsToNextLevel = (nextThreshold - totalPoints).coerceAtLeast(0),
      levelProgress = progressPercent,
      badges = generateBadges(quizzesCreated, pollsCreated, commentsContributed)
    )
  }
}
