package com.btl.tinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.compose.ui.channels.ChannelsScreen
import io.getstream.chat.android.compose.ui.theme.ChatTheme
import io.getstream.chat.android.models.InitializationState
import io.getstream.chat.android.models.User
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory

@Composable
fun ChatListScreen(navController: NavController) {
    val context = LocalContext.current

    // 1. Khởi tạo plugin offline & state (chỉ 1 lần)
    val offlinePluginFactory = remember {
        StreamOfflinePluginFactory(appContext = context.applicationContext)
    }
    val statePluginFactory = remember {
        StreamStatePluginFactory(config = StatePluginConfig(), appContext = context)
    }

    // 2. Tạo client Stream Chat (chỉ 1 lần)
    val client = remember {
        ChatClient.Builder("ghhjw753ksej", context.applicationContext)
            .withPlugins(offlinePluginFactory, statePluginFactory)
            .logLevel(ChatLogLevel.ALL)
            .build()
    }

    // 3. Kết nối user khi composable được load
    LaunchedEffect(Unit) {
        val user = User(
            id = "namtest",
            name = "namtest",
            image = "https://bit.ly/2TIt8NR"
        )

        client.connectUser(
            user = user,
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibmFtdGVzdCJ9.qxisoh2Kj08Q1GDdf3uICX34a66MbKjgB1V7e24VIZU"
        ).enqueue()
    }

    // 4. Theo dõi trạng thái khởi tạo client
    val clientInitializationState by client.clientState.initializationState.collectAsState()

    // 5. Giao diện hiển thị
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            when (clientInitializationState) {
                InitializationState.COMPLETE -> {
                    ChannelsScreen(
                        onChannelClick = { channel ->
                            // TODO: điều hướng sang màn hình chat cụ thể
                            // navController.navigate("chat/${channel.cid}")
                        },
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }

                InitializationState.INITIALIZING -> {
                    Text(text = "Initializing...")
                }

                InitializationState.NOT_INITIALIZED -> {
                    Text(text = "Not initialized...")
                }
            }

            // Giữ nguyên thanh bottom navigation của bạn
            BottomNavigationMenu(
                selectedItem = BottomNavigationItem.CHATLIST,
                navController = navController
            )
        }
}
