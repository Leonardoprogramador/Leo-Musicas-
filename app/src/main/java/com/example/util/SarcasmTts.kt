package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.MusicPlayerManager
import java.util.Locale

class SarcasmTts(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("SarcasmTts", "Portuguese language (PT-BR) is not supported or missing data, falling back to default.")
                tts?.setLanguage(Locale.getDefault())
            }
            // Normal speech rate and pitch
            tts?.setSpeechRate(1.0f)
            tts?.setPitch(1.0f)
            
            // Set up Utterance Progress Listener for volume ducking
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    try {
                        MusicPlayerManager.getInstance(context).setTemporaryDucking(true)
                    } catch (e: Exception) {
                        Log.e("SarcasmTts", "Error during ducking onStart: ${e.message}")
                    }
                }

                override fun onDone(utteranceId: String?) {
                    try {
                        MusicPlayerManager.getInstance(context).setTemporaryDucking(false)
                    } catch (e: Exception) {
                        Log.e("SarcasmTts", "Error during ducking onDone: ${e.message}")
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    try {
                        MusicPlayerManager.getInstance(context).setTemporaryDucking(false)
                    } catch (e: Exception) {
                        Log.e("SarcasmTts", "Error during ducking onError: ${e.message}")
                    }
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    super.onStop(utteranceId, interrupted)
                    try {
                        MusicPlayerManager.getInstance(context).setTemporaryDucking(false)
                    } catch (e: Exception) {
                        Log.e("SarcasmTts", "Error during ducking onStop: ${e.message}")
                    }
                }
            })

            isInitialized = true
        } else {
            Log.e("SarcasmTts", "Failed to initialize TextToSpeech engine.")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            try {
                // Ensure music ducking happens immediately
                MusicPlayerManager.getInstance(context).setTemporaryDucking(true)
                // QUEUE_FLUSH replaces any current text being read with the new one immediately
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SarcasmTtsSpeechId")
            } catch (e: Exception) {
                Log.e("SarcasmTts", "Error during tts speak: ${e.message}")
            }
        } else {
            Log.w("SarcasmTts", "TTS not fully initialized yet.")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("SarcasmTts", "Error shutting down TextToSpeech: ${e.message}")
        } finally {
            tts = null
            isInitialized = false
        }
    }
}
