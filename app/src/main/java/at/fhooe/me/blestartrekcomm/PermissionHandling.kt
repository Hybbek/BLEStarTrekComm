package at.fhooe.me.blestartrekcomm

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val RUNTIME_PERMISSION_REQUEST_CODE = 12 //request code corresponding to the runtime-permission-enabling action


/**
 * extension function to check if there are permissions which have to be required.
 */
fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permissionType) ==
            PackageManager.PERMISSION_GRANTED
}

/**
 * extension function to check if the required runtime permissions are given.
 */
fun Context.hasRequiredRuntimePermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

/**
 * Checks if required runtime permissions are given.
 * If not, asking the user to manually give permissions.
 */
fun Activity.requestRelevantRuntimePermissions() {
    val permissionHandling = PermissionHandling()
    if (hasRequiredRuntimePermissions()) { return }
    when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
            permissionHandling.requestLocationPermission()
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            permissionHandling.requestBluetoothPermissions()
        }
    }
}


class PermissionHandling : Activity() {

    // Variable to store if local permissions are granted
    private val mIsLocationPermissionGranted get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    /**
     * Alert which asks for location permission.
     */
    fun requestLocationPermission() {
        if (mIsLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Location permission required")
            builder.setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                    "location access in order to scan for BLE devices.")
            builder.setCancelable(false)

            builder.setPositiveButton(android.R.string.ok) { dialog, which ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    RUNTIME_PERMISSION_REQUEST_CODE
                )
            }
            builder.show()
        }
    }

    /**
     * Alert which asks for bluetooth permissions.
     */
    fun requestBluetoothPermissions() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Bluetooth permissions required")
            builder.setMessage("Starting from Android 12, the system requires apps to be granted " +
                    "Bluetooth access in order to scan for and connect to BLE devices.")
            builder.setCancelable(false)
            builder.setPositiveButton(android.R.string.ok){ dialog, which ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ),
                        RUNTIME_PERMISSION_REQUEST_CODE
                    )
                }
            }
            builder.show()
        }
    }
}