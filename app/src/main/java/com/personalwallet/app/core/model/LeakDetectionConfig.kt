package com.personalwallet.app.core.model

data class LeakDetectionConfig(
    val thresholdMicroExpenseAmount: Long = 25000L,
    val thresholdMicroExpenseWeeklyCount: Int = 3,
    val thresholdPayLaterRatio: Float = 0.20f,
    val thresholdImpulseDevMultiplier: Float = 2.0f
)
