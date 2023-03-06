package at.fhooe.me.blestartrekcomm

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.util.*

//val APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
val APP_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
val CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
val DESC_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

class BLEConnection : Activity() {

}