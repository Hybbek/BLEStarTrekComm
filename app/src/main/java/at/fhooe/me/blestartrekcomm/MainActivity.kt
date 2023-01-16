package at.fhooe.me.blestartrekcomm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 11 //request code corresponding to the Bluetooth-enabling action
private const val RUNTIME_PERMISSION_REQUEST_CODE = 12 //request code corresponding to the runtime-permission-enabling action

/**
 *
 */
@SuppressLint("MissingPermission") // App's role to ensure permissions are available
class MainActivity : AppCompatActivity() {
    lateinit var mScanButton: Button  //Scan Button
    lateinit var mlistOfDevices: ListView //ListView to list BLE devices
    lateinit var mListAdapter: ArrayAdapter<String> //List with available devices

    var mSocket: BTConnection? = null //Kommunikationssocket
    val mDevicesAddresses = ArrayList<String>() //Liste mit den Adressen der Geräten


    //Variables are lazy bc they need to be checked at runtime!!

    //Ble scanner Object
    private val mBleScanner by lazy {
        mBluetoothAdapter.bluetoothLeScanner
    }

    // Local device Bluetooth Adapter
    private val mBluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Specific scan setting
    private val mScanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // Variable to store if local permissions are granted
    private val mIsLocationPermissionGranted get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    // Value if program is scanning or not. With setter method which changes the button text.
    private var mIsScanning = false
        set(value) {
            field = value
            runOnUiThread { mScanButton.text = if (value) "Stop Scan" else "Scan" }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mlistOfDevices = findViewById(R.id.list_devices)
        mScanButton = findViewById(R.id.refresh_button)

        mListAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        mlistOfDevices.adapter = mListAdapter

        //Make List Objects Clickable
        mlistOfDevices.setOnItemClickListener { adapterView, view, position, id ->
            val address = mDevicesAddresses[position]
            Toast.makeText(this@MainActivity, "Device with address $address selected", Toast.LENGTH_SHORT).show()
            val device = mBluetoothAdapter.getRemoteDevice(address)
            val devName = device.name
            val devAddr = device.address
            mSocket = BTConnection(mBluetoothAdapter,device,this.applicationContext)
            mSocket!!.start()
            Toast.makeText(this@MainActivity, "$devName $devAddr found", Toast.LENGTH_SHORT).show()
        }

        mScanButton.setOnClickListener{
            if (!mIsScanning)
                startBleScan()
            else
                stopBleScan()
             }
    }



    override fun onResume() {
        super.onResume()
        if (!mBluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            with(result.device) {

                Log.i(
                    "ScanCallback",
                    "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address"
                )
            if (!mDevicesAddresses.contains(result.device.address)){
                        mDevicesAddresses.add("$address")
                        mListAdapter.add("$name ($address)")
                        mListAdapter.notifyDataSetChanged()
                    }

                }

        }

        override fun onScanFailed(errorCode: Int) {
            Log.i("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    /**
     * If bluetooth isn't enabled on the device this method displays an alert to enable Bluetooth.
     */
    private fun promptEnableBluetooth() {
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }





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
     * Checks if the required runtime permissions have been granted before allowing to proceed with
     * the BLE scan.
     * If so, the BLE scan starts
     */
    private fun startBleScan() {
        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        } else {
            mDevicesAddresses.clear()
            mListAdapter.notifyDataSetChanged()
            mBleScanner.startScan(null, mScanSettings, scanCallback)
            mIsScanning = true
        }

    }

    /**
     * Stops the BLE scan.
     */
    private fun stopBleScan() {
        mBleScanner.stopScan(scanCallback)
        mIsScanning = false
    }


    /**
     * Checks if required runtime permissions are given.
     * If not, asking the user to manually give permissions.
     */
    private fun Activity.requestRelevantRuntimePermissions() {
        if (hasRequiredRuntimePermissions()) { return }
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                requestLocationPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }

    }

    /**
     * Alert which asks for location permission.
     */
    private fun requestLocationPermission() {
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
    private fun requestBluetoothPermissions() {
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
           RUNTIME_PERMISSION_REQUEST_CODE -> {
               val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
                   it.second == PackageManager.PERMISSION_DENIED &&
                           !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
               }
               val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
               val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

               when{
                   containsPermanentDenial -> {
                       // TODO: Handle permanent denial (e.g., show AlertDialog with justification)
                       // Note: The user will need to navigate to App Settings and manually grant
                       // permissions that were permanently denied
                   }
                   containsDenial -> {
                       requestRelevantRuntimePermissions()
                   }
                   allGranted && hasRequiredRuntimePermissions() -> {
                       startBleScan()
                   }
                   else -> {
                       // Unexpected scenario encountered when handling permissions
                       recreate()
                   }
               }
           }
        }
    }

}