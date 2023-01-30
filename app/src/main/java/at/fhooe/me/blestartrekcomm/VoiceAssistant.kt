package at.fhooe.me.blestartrekcomm

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager
import com.android.internal.telephony.ITelephony
import java.lang.Exception
import java.lang.reflect.Method

class VoiceAssistant (_context: Context){
    private val mContext = _context

    /**
     * Starts the Google Voice Assistant
     */
    fun activateAssistant(){
        // check if phone is in sleep mode

        // starts Google Voice assistant
        mContext.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Ends the current phone call
     */
    fun endPhoneCall(){
        // ends phone call
        try {
            //Ends Phone connection
            val telephonyManager =
                mContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val clazz = Class.forName(telephonyManager.javaClass.name)
            val method: Method = clazz.getDeclaredMethod("getITelephony")
            method.isAccessible = true
            val telephonyService: ITelephony = method.invoke(telephonyManager) as ITelephony
            telephonyService.endCall()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    /**
     * Checks if a call is active
     * @return true if a call is active, false otherwise
     */
    fun isCallActive(): Boolean {
        val manager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return manager.mode == AudioManager.MODE_IN_CALL
    }

}