package com.btl.tinder

import androidx.navigation.NavController

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