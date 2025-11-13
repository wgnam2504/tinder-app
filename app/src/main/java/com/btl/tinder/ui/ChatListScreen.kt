package com.btl.tinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.btl.tinder.TCViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.compose.ui.channels.ChannelsScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.models.InitializationState
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height


@Composable
fun ChatListScreen(navController: NavController, vm: TCViewModel) {
    val context = LocalContext.current

    val offlinePluginFactory = remember {
        StreamOfflinePluginFactory(appContext = context.applicationContext)
    }
    val statePluginFactory = remember {
        StreamStatePluginFactory(config = StatePluginConfig(), appContext = context.applicationContext)
    }

    val client = remember {
        ChatClient.Builder("ghhjw753ksej", context.applicationContext)
            .withPlugins(offlinePluginFactory, statePluginFactory)
            .logLevel(ChatLogLevel.ALL)
            .build()
    }

    val userData = vm.userData.value
    val userId = userData?.userId ?: "unknown_user"
    val username = userData?.username ?: "Unknown"
    val imageUrl = userData?.imageUrl ?: "https://bit.ly/2TIt8NR"





    LaunchedEffect(userId) {
        val user = User(
            id = userId,
            name = username,
            image = imageUrl
        )
        val token = client.devToken(user.id)

        client.connectUser(user, token).enqueue()
    }

    val clientInitializationState by client.clientState.initializationState.collectAsState()

    // ✅ Bọc toàn bộ trong Box
    Box(modifier = Modifier.fillMaxSize()) {

        // Nội dung chính
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp) // để chừa chỗ cho bottom bar
        ) {
            when (clientInitializationState) {
                InitializationState.COMPLETE -> {
                    ChatTheme {
                        ChannelsScreen(
                            onChannelClick = { channel ->
                                // navController.navigate("chat/${channel.cid}")
                            },
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                }

                InitializationState.INITIALIZING -> {
                    Text(text = "Initializing...")
                }

                InitializationState.NOT_INITIALIZED -> {
                    Text(text = "Not initialized...")
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            BottomNavigationMenu(
                selectedItem = BottomNavigationItem.CHATLIST,
                navController = navController
            )
        }
    }
}

