package org.stypox.dicio.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.shreyaspatil.permissionflow.compose.rememberMultiplePermissionState
import org.dicio.skill.skill.Permission
import org.stypox.dicio.R

val PERMISSION_READ_CONTACTS = Permission.NormalPermission(
    name = R.string.perm_read_contacts,
    id = Manifest.permission.READ_CONTACTS,
)
val PERMISSION_CALL_PHONE = Permission.NormalPermission(
    name = R.string.perm_call_phone,
    id = Manifest.permission.CALL_PHONE,
)

val PERMISSION_NOTIFICATION_LISTENER = Permission.SecurePermission(
    name = R.string.perm_notification_listener,
    id = "enabled_notification_listeners",
    settingsAction = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
)

/**
 * @param context the Android context
 * @param permissions an array of permissions to check (can be empty)
 * @return true if ALL of the provided permissions are granted when checking with
 * [ActivityCompat.checkSelfPermission], false otherwise. So returns
 * true if the provided permissions array is empty.
 */
fun checkPermissions(context: Context, vararg permissions: String): Boolean {
    for (permission in permissions) {
        if (ActivityCompat.checkSelfPermission(context, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }
    return true
}

/**
 * @param grantResults an array of permission grant results to check (can be empty)
 * @return true if ALL of the provided grant results are equal to
 * [PackageManager.PERMISSION_GRANTED], false otherwise. So returns true if the provided grant
 * results array is empty.
 */
fun areAllPermissionsGranted(vararg grantResults: Int): Boolean {
    for (grantResult in grantResults) {
        if (grantResult != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}

/**
 * Filters the given permissions by whether they are granted or not.
 */
fun getNonGrantedSecurePermissions(
    context: Context,
    permissions: List<Permission.SecurePermission>
): List<Permission> {
    return permissions.filter {
        Settings.Secure.getString(context.contentResolver, it.id)
            ?.contains(context.packageName) != true
    }
}

/**
 * Returns the list of non-granted permissions. To be used in Compose. Will hold state within itself
 * to connect to the `PermissionFlow` using [rememberMultiplePermissionState] and to keep track of
 * secure settings grant state after resuming the app.
 */
@Composable
fun getNonGrantedPermissions(permissions: List<Permission>): List<Permission> {
    val normalPermissions = permissions.filterIsInstance<Permission.NormalPermission>()
    val securePermissions = permissions.filterIsInstance<Permission.SecurePermission>()

    val normal = if (normalPermissions.isEmpty()) {
        listOf<Permission>()
    } else {
        val permissionsState by
            rememberMultiplePermissionState(*(normalPermissions.map { it.id }.toTypedArray()))
        normalPermissions
            .zip(permissionsState.permissions)
            .filter { !it.second.isGranted }
            .map { it.first }
    }

    val secure = if (securePermissions.isEmpty()) {
        listOf()
    } else {
        val context = LocalContext.current
        var secureNonGranted by remember {
            mutableStateOf(getNonGrantedSecurePermissions(context, securePermissions))
        }
        LifecycleResumeEffect(null) {
            secureNonGranted = getNonGrantedSecurePermissions(context, securePermissions)
            onPauseOrDispose {}
        }
        secureNonGranted
    }

    return normal + secure
}

/**
 * @param context the Android context
 * @param permissions the list of permissions; their localized names will be joined with commas
 * @return comma-separated list of the localized labels of all of the permissions
 */
fun commaJoinPermissions(context: Context, permissions: List<Permission>): String {
    return permissions.joinToString(", ") { context.getString(it.name) }
}

typealias PermissionLauncher = ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>

/**
 * Makes only one permission request: all normal permissions are requested in one go, while only one
 * secure permission is requested.
 */
fun requestAnyPermission(
    launcher: PermissionLauncher,
    context: Context,
    permissions: List<Permission>,
) {
    val normalPermissions = permissions.filterIsInstance<Permission.NormalPermission>()
    val securePermissions = permissions.filterIsInstance<Permission.SecurePermission>()

    if (normalPermissions.isNotEmpty()) {
        launcher.launch(normalPermissions.map { it.id }.toTypedArray())
        return // only launch one thing at a time
    }

    // only launch one thing at a time
    val action = securePermissions.firstOrNull()?.settingsAction ?: return
    context.startActivity(Intent(action))
}

