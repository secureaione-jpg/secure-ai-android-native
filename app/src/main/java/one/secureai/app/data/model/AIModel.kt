package one.secureai.app.data.model

enum class SubscriptionTier(val rank: Int, val wireValue: String) {
    FREE(0, "free"),
    PLUS(1, "plus"),
    BUSINESS(2, "business"),
    PRO(3, "pro"),
    ULTRA(4, "ultra");

    fun allowsPremiumModels() = rank >= PLUS.rank
    fun allowsFrontierModels() = rank >= PRO.rank
    fun allowsAttachments() = rank >= PLUS.rank
}

enum class AIModel(
    val wireValue: String,
    val displayName: String,
    val requiredTier: SubscriptionTier
) {
    AUTO("secureai-auto", "Auto", SubscriptionTier.FREE),
    PLUS("secureai-plus", "Plus", SubscriptionTier.PLUS),
    PRO("secureai-pro", "Pro", SubscriptionTier.PRO),
    ULTRA("secureai-ultra", "Ultra", SubscriptionTier.ULTRA);

    fun isAccessible(userTier: SubscriptionTier): Boolean =
        userTier.rank >= requiredTier.rank
}
