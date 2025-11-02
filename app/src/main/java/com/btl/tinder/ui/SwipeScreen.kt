package com.btl.tinder.ui

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.btl.tinder.CommonImage
import com.btl.tinder.CommonProgressSpinner
import com.btl.tinder.R
import com.btl.tinder.TCViewModel
import com.btl.tinder.data.UserData
import com.btl.tinder.swipecards.Direction
import com.btl.tinder.swipecards.rememberSwipeableCardState
import com.btl.tinder.swipecards.swipableCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import meshGradient

@Composable
fun SwipeScreen(navController: NavController, vm: TCViewModel) {
    val inProgress = vm.inProgressProfiles.value

    val animatedPoint = remember { Animatable(.8f) }
    LaunchedEffect(Unit) {
        while (true) {
            animatedPoint.animateTo(
                targetValue = .1f,
                animationSpec = tween(durationMillis = 10000)
            )
            animatedPoint.animateTo(
                targetValue = .9f,
                animationSpec = tween(durationMillis = 10000)
            )
        }
    }

    if (inProgress)
        CommonProgressSpinner()
    else {
        val profiles = vm.matchProfiles.value
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .meshGradient(
                    points = listOf(
                        listOf(
                            Offset(0f, 0f) to Color.White,
                            Offset(.5f, 0f) to Color.White,
                            Offset(1f, 0f) to Color.White,
                        ),
                        listOf(
                            Offset(0f, .5f) to Color(0xFFFF7898),
                            Offset(.5f, animatedPoint.value) to Color(0xFFFF7898),
                            Offset(1f, .5f) to Color(0xFFFF7898),
                        ),
                        listOf(
                            Offset(0f, 1f) to Color(0xFF744D8C),
                            Offset(.5f, 1f) to Color(0xFF744D8C),
                            Offset(1f, 1f) to Color(0xFF744D8C),
                        ),
                    ), resolutionY = 32
                )
        ) {
            // Spacer
            Spacer(modifier = Modifier.height(1.dp))

            // Cards
            val states = profiles.map { it to rememberSwipeableCardState() }
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .aspectRatio(0.9f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "No more profiles available")
                }
                states.forEach { (matchProfile, state) ->
                    ProfileCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .swipableCard(
                                state = state,
                                blockedDirections = listOf(Direction.Down),
                                onSwiped = {},
                                onSwipeCancel = { Log.d("Swipeable card", "Cancelled swipe") }),
                        matchProfile = matchProfile
                    )
                    LaunchedEffect(matchProfile, state.swipedDirection) {
                        if (state.swipedDirection != null) {
                            if (state.swipedDirection == Direction.Left ||
                                state.swipedDirection == Direction.Down
                            ) {
                                vm.onDislike(matchProfile)
                            } else {
                                vm.onLike(matchProfile)
                            }
                        }
                    }
                }
            }

            // Buttons
            val scope = rememberCoroutineScope()
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircleButton(onClick = {
                    scope.launch {
                        val last = states.reversed().firstOrNull {
                            it.second.offset.value == Offset(0f, 0f)
                        }?.second
                        last?.swipe(Direction.Left)
                    }
                }, drawableResId = R.drawable.cancel, backgroundColor = Color(0xFFE91E63))
                CircleButton(onClick = {
                    scope.launch {
                        val last = states.reversed().firstOrNull {
                            it.second.offset.value == Offset(0f, 0f)
                        }?.second
                        last?.swipe(Direction.Right)
                    }
                }, drawableResId = R.drawable.love, backgroundColor = Color(0xFF673AB7))
            }

            // Bottom nav bar
            BottomNavigationMenu(
                selectedItem = BottomNavigationItem.SWIPE,
                navController = navController
            )
        }
    }
}

@Composable
private fun CircleButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    drawableResId: Int,
    backgroundColor: Color
) {
    IconButton(
        modifier = Modifier
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            ) // Added shadow modifier
            .clip(CircleShape)
            .background(Color.White)
            .size(89.dp)
            .border(5.dp, backgroundColor, CircleShape),
        onClick = onClick
    ) {
//        Icon(
//            icon, null,
//            tint = MaterialTheme.colorScheme.onPrimary
//        )
        Image(
            painter = painterResource(id = drawableResId),
            contentDescription = "Button icon",
            modifier = Modifier.aspectRatio(0.5f, true),
            alignment = Alignment.Center,
            contentScale = ContentScale.Inside,
        )
    }
}

@Composable
private fun ProfileCard(
    modifier: Modifier,
    matchProfile: UserData,
) {
    Card(modifier) {
        Box {
            CommonImage(matchProfile.imageUrl, modifier = Modifier.fillMaxSize())
            Scrim(Modifier.align(Alignment.BottomCenter))
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(text = matchProfile.name ?: matchProfile.username ?: "",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = deliusFontFamily,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp))
                if (!matchProfile.bio.isNullOrEmpty()) {
                    Text(text = matchProfile.bio!!,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = playpenFontFamily,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun Scrim(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .height(180.dp)
            .fillMaxWidth())
}
