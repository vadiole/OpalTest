package vadiole.opaltest.feature.referral

import androidx.annotation.DrawableRes
import vadiole.opaltest.R
import vadiole.opaltest.domain.referral.MilestoneStone

@get:DrawableRes
val MilestoneStone.thumbnail: Int
    get() = when (this) {
        MilestoneStone.Loyal -> R.drawable.img_milestone_loyal
        MilestoneStone.Soulful -> R.drawable.img_milestone_soulful
        MilestoneStone.Popular -> R.drawable.img_milestone_popular
    }

@get:DrawableRes
val MilestoneStone.revealImage: Int
    get() = when (this) {
        MilestoneStone.Loyal -> R.drawable.img_milestone_loyal_large
        MilestoneStone.Soulful -> R.drawable.img_milestone_soulful_large
        MilestoneStone.Popular -> R.drawable.img_milestone_popular_large
    }
