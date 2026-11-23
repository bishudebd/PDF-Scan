package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueSoft
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val titleRes: Int,
    val descRes: Int,
    val badge: String,
    @DrawableRes val imageRes: Int
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_title_1,
            descRes = R.string.onboarding_desc_1,
            badge = "SMART CAPTURE",
            imageRes = R.drawable.img_onboarding_scan_1795327148052
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_2,
            descRes = R.string.onboarding_desc_2,
            badge = "STUDIO ENHANCEMENT",
            imageRes = R.drawable.img_onboarding_enhance_1795327145053
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_3,
            descRes = R.string.onboarding_desc_3,
            badge = "OFFLINE PDF SUITE",
            imageRes = R.drawable.img_onboarding_organize_1795327147063
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = onComplete,
                modifier = Modifier.testTag("onboarding_skip_button")
            ) {
                Text(stringResource(R.string.onboarding_skip), color = CobaltBlue)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIdx ->
            val page = pages[pageIdx]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                ) {
                    Image(
                        painter = painterResource(id = page.imageRes),
                        contentDescription = stringResource(page.titleRes),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CobaltBlueSoft
                ) {
                    Text(
                        text = page.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = CobaltBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(page.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(page.descRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Animated page indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 18.dp)
        ) {
            repeat(pages.size) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (active) 28.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (active) CobaltBlue else MaterialTheme.colorScheme.outline)
                )
            }
        }

        val isLast = pagerState.currentPage == pages.lastIndex
        Button(
            onClick = {
                if (isLast) {
                    onComplete()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("onboarding_next_button")
        ) {
            Text(
                text = if (isLast) stringResource(R.string.onboarding_get_started) else stringResource(R.string.onboarding_next),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
