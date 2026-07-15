package one.secureai.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.secureai.app.R

private data class OnboardingPage(
    val icon: Int,
    val titleRes: Int,
    val bodyRes: Int,
    val accent: Color = Color(0xFF2563EB)
)

private val PAGES = listOf(
    OnboardingPage(
        icon = R.drawable.ic_sparkle,
        titleRes = R.string.onboarding_title_1,
        bodyRes = R.string.onboarding_body_1
    ),
    OnboardingPage(
        icon = R.drawable.ic_lock,
        titleRes = R.string.onboarding_title_2,
        bodyRes = R.string.onboarding_body_2
    ),
    OnboardingPage(
        icon = R.drawable.ic_document,
        titleRes = R.string.onboarding_title_3,
        bodyRes = R.string.onboarding_body_3
    ),
    OnboardingPage(
        icon = R.drawable.ic_folder,
        titleRes = R.string.onboarding_title_4,
        bodyRes = R.string.onboarding_body_4
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0C))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Skip
        TextButton(
            onClick = onFinish,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Text("Skip", color = Color(0xFF8E8E93), fontSize = 15.sp)
        }

        // Page content
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 40.dp),
            label = "onboarding"
        ) { p ->
            val current = PAGES[p]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1A1A1C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(current.icon),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = current.accent
                    )
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    text = stringResource(current.titleRes),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F5F7),
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(current.bodyRes),
                    fontSize = 16.sp,
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PAGES.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == page) 20.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (i == page) Color(0xFF2563EB) else Color(0xFF3A3A3C))
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { if (page < PAGES.lastIndex) page++ else onFinish() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(
                    text = if (page < PAGES.lastIndex) stringResource(R.string.continue_text) else stringResource(R.string.get_started),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
