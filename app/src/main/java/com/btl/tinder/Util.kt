package com.btl.tinder

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

/**
 * Điều hướng đến một màn hình cụ thể trong ứng dụng thông qua [NavController].
 *
 * Hàm này chuyển đến route được chỉ định và đảm bảo rằng:
 * - Nếu màn hình (route) đó đã có trong back stack, nó sẽ không được tạo thêm (nhờ `launchSingleTop = true`).
 * - Các màn hình nằm phía trên route đó trong back stack sẽ bị xóa (nhờ `popUpTo(route)`),
 *   giúp tránh trùng lặp và giữ ngăn xếp điều hướng gọn gàng.
 *
 * @param navController Bộ điều khiển điều hướng dùng để thực hiện chuyển màn hình.
 * @param route Đường dẫn (route) của màn hình đích cần điều hướng tới.
 */
fun navigateTo(navController: NavController, route: String) {
    navController.navigate(route) {
        popUpTo(route)
        launchSingleTop = true
    }
}

@Composable
fun CommonProgressSpinner() {
    Row(
        modifier = Modifier
            .alpha(0.5f)
            .background(Color.LightGray)
            .clickable(enabled = false) {}
            .fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun NotificationMessage(vm: TCViewModel) {
    val notifState = vm.popupNotification.value
    val activity = LocalContext.current as? Activity

    val boldTypeface = Typeface.create(
        ResourcesCompat.getFont(LocalContext.current, R.font.delius_regular),
        Typeface.BOLD
    )

    LaunchedEffect(notifState) {
        notifState?.getContentOrNull()?.let { notifMessage ->
            activity?.let {
                MotionToast.darkToast(
                    it,
                    "Error ☹️",
                    notifMessage,
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    boldTypeface
                )
            }
        }
    }
}