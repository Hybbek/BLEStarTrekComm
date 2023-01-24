package at.fhooe.me.blestartrekcomm

import android.annotation.SuppressLint
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
val DESC_UUID = UUID.fromString("beb5483e-36e1-4688-c7f5-ea07361b26a9")
@SuppressLint("MissingPermission")
class BTConnection (private val mBluetoothAdapter: BluetoothAdapter, private val device: BluetoothDevice, private val _context: Context) : Thread(){
    private val mmClientSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(APP_UUID)
    }

    override fun run() {
        // Hört weiter, bis eine Ausnahme auftritt oder ein Socket zurückgegeben wird.
        mBluetoothAdapter.cancelDiscovery()

        mmClientSocket?.let { socket ->
            // stellt die Verbindung her
            socket.connect()

            Toast.makeText(_context, "Connected", Toast.LENGTH_SHORT).show()

            // Öffnet die Kommunikatiion
            //manageMyConnectedSocket(socket)
        }
    }

    /**
     * Ermöglicht den beiden Geräten die Bluetoothkommunikation zu starten.
     */
    fun manageMyConnectedSocket(socket: BluetoothSocket){
        //var transferData = BluetoothDataTransfer(socket,_context)
        //transferData.start()
    }

    /**
     * Schliesst den Vrbindungssocket und beendet den Thread.
     */
    fun cancel() {
        try {
            mmClientSocket?.close()
        } catch (e: IOException) {
            Log.e(ContentValues.TAG, "Could not close the connect socket", e)
        }
    }

}