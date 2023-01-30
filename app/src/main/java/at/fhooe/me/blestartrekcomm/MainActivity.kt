package at.fhooe.me.blestartrekcomm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.nio.charset.Charset
import java.util.*


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 11 //request code corresponding to the Bluetooth-enabling action
private const val RUNTIME_PERMISSION_REQUEST_CODE = 12 //request code corresponding to the runtime-permission-enabling action

/**
 *
 */
@SuppressLint("MissingPermission") // App's role to ensure permissions are available
class MainActivity : AppCompatActivity() {
    val mVoiceAssistant = VoiceAssistant(this) // Voice assistant object

    lateinit var mScanButton: Button  //Scan Button
    lateinit var mlistOfDevices: ListView //ListView to list BLE devices
    lateinit var mListAdapter: ArrayAdapter<String> //List with available devices

    //var mSocket: BTConnection? = null //Kommunikationssocket
    val mDevicesAddresses = ArrayList<String>() //Liste mit den Adressen der Geräten
    var mBluetoothGatt: BluetoothGatt? = null //Connection to Gatt server

    private var bluetoothService : BluetoothLeService? = null //Bluetooth Service

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
            Toast.makeText(this@MainActivity, "$devName $devAddr found", Toast.LENGTH_SHORT).show()
            val isConnected = connect(device)
            Toast.makeText(this@MainActivity, "Connected to $devName", Toast.LENGTH_SHORT).show()

            //mSocket = BTConnection(mBluetoothAdapter,device,this.applicationContext)
            //mSocket!!.start()

        }

        mScanButton.setOnClickListener{
            if (!mIsScanning)
                startBleScan()
            else
                stopBleScan()
             }
    }


    /*private fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }
        if (mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            return mBluetoothGatt!!.connect()
        }
        val device = mBluetoothAdapter
            .getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        mBluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        return mBluetoothGatt!!.connect()
    }*/
    private fun connect(device: BluetoothDevice) {
        mBluetoothAdapter?.let { adapter ->
            try {
                // connect to the GATT server on the device
                mBluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                mBluetoothGatt!!.connect()  // connect to the GATT server on the device

            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.  Unable to connect.")
            }
        }
    }


    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                Log.d(TAG, "Connected to GATT server.")
                // discover services on the GATT Server
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                Log.d(TAG, "Disconnected from GATT server.")
                mBluetoothGatt?.close()
                mBluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // successfully discovered services
                Log.d(TAG, "Discovered GATT services.")
                val service = gatt?.getService(APP_UUID)
                mBluetoothGatt?.let { gatt ->
                    val characteristic = service?.getCharacteristic(CHAR_UUID)
                    readCharacteristic(service!!.getCharacteristic(CHAR_UUID))
                    if (characteristic != null) {
                        setCharacteristicNotification(characteristic, true)
                    }
                    characteristic?.getDescriptor(DESC_UUID)?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        Log.d(TAG, "Write GATT descriptor: ${it.value?.contentToString()}")
                        gatt.writeDescriptor(it)
                    }
                }

            } else {
                // failed to discover services
                Log.d(TAG, "Failed to discover GATT services.")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic?.value
                if (value != null && value.isNotEmpty()){
                    //setCharacteristicNotification(characteristic, true)
                    //convert byte array to string
                    val str = String(value, Charset.forName("UTF-8"))
                    Log.d(TAG, "Read GATT characteristic: $str")
                }else {
                    Log.d(TAG, "Read GATT characteristic: null")
                }

                // successfully read characteristic
                Log.d(TAG, "Read GATT characteristic.")
            } else {
                // failed to read characteristic
                Log.d(TAG, "Failed to read GATT characteristic.")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val value = characteristic?.value
            if (value != null && value.isNotEmpty()) {
                //setCharacteristicNotification(characteristic, true)
                //convert byte array to string
                val str = String(value, Charset.forName("UTF-8"))
                if (str == "Emblem pressed") {
                    if (mVoiceAssistant.isCallActive()){
                        mVoiceAssistant.endPhoneCall()
                    }else{
                        mVoiceAssistant.activateAssistant()
                        //Log.d(TAG, "Emblem pressed")
                    }
                }else{
                    Log.d(TAG, "Read GATT characteristic: $str")
                }
            } else {
                Log.d(TAG, "Read GATT characteristic: null")
            }

            /*value?.let { val str = String(it, Charset.forName("UTF-8"))
                val i = 0;
                //Log.d(TAG, "Characteristic changed $str")

            }*/
            // characteristic value changed

        }
    }

    /**
     * Reads the value of a given characteristic.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.readCharacteristic(characteristic)
    }

    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
        Log.d(TAG, "Set characteristic notification: $enabled")
        if(CHAR_UUID == characteristic.uuid){
            val descriptor = characteristic.getDescriptor(DESC_UUID)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt!!.writeDescriptor(descriptor)
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