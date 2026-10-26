
package com.example.domaassistente

import android.app.Activity
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var audioManager: AudioManager
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var statusText: TextView

    private var ttsReady = false
    private var audioCallbackRegistrado = false

    private val audioCallback = object : AudioDeviceCallback() {

        override fun onAudioDevicesAdded(
            addedDevices: Array<out AudioDeviceInfo>
        ) {
            atualizarAudio()
        }

        override fun onAudioDevicesRemoved(
            removedDevices: Array<out AudioDeviceInfo>
        ) {
            atualizarAudio()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager =
            getSystemService(AUDIO_SERVICE) as AudioManager

        textToSpeech = TextToSpeech(this, this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
        }

        val title = TextView(this).apply {
            text = "Doma Assistente"
            textSize = 24f
        }

        statusText = TextView(this).apply {
            text = "Pronto para verificar o áudio."
            textSize = 16f
            setPadding(0, 24, 0, 24)
        }

        val checkButton = Button(this).apply {
            text = "Verificar áudio"
            setOnClickListener {
                verificarAudio()
            }
        }

        val speakButton = Button(this).apply {
            text = "Ouvir mensagem"
            setOnClickListener {
                falar("Bem-vindo ao Doma Assistente.")
            }
        }

        val alertButton = Button(this).apply {
            text = "Testar alerta"
            setOnClickListener {
                falar("Atenção. Este é um alerta de segurança.")
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(checkButton)
        layout.addView(speakButton)
        layout.addView(alertButton)

        val scrollView = ScrollView(this)
        scrollView.addView(layout)
        setContentView(scrollView)

        verificarAudio()
    }

    override fun onResume() {
        super.onResume()

        audioManager.registerAudioDeviceCallback(
            audioCallback,
            Handler(Looper.getMainLooper())
        )
        audioCallbackRegistrado = true
    }

    override fun onPause() {
        if (::audioManager.isInitialized &&
            audioCallbackRegistrado
        ) {
            audioManager.unregisterAudioDeviceCallback(
                audioCallback
            )
            audioCallbackRegistrado = false
        }

        super.onPause()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(
                Locale("pt", "BR")
            )

            ttsReady =
                result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun verificarAudio() {
        atualizarAudio()
    }

    private fun atualizarAudio() {
        if (!::audioManager.isInitialized ||
            !::statusText.isInitialized
        ) {
            return
        }

        val saidas = audioManager.getDevices(
            AudioManager.GET_DEVICES_OUTPUTS
        )

        val altoFalante = saidas.any {
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }

        val foneBluetooth = saidas.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        }

        val resultado = """
            Saídas de áudio: ${saidas.size}
            Alto-falante integrado: ${
            if (altoFalante) "Disponível" else "Não detectado"
        }
            Fone Bluetooth A2DP: ${
            if (foneBluetooth) "Detectado" else "Não detectado"
        }

            Monitoramento de conexões ativo.
        """.trimIndent()

        statusText.text = resultado
    }

    private fun falar(mensagem: String) {
        if (ttsReady) {
            textToSpeech.speak(
                mensagem,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "doma_mensagem"
            )
        } else {
            statusText.text =
                "O serviço de voz ainda não está pronto ou não está disponível."
        }
    }

    override fun onDestroy() {
        if (::audioManager.isInitialized &&
            audioCallbackRegistrado
        ) {
            audioManager.unregisterAudioDeviceCallback(
                audioCallback
            )
            audioCallbackRegistrado = false
        }

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        super.onDestroy()
    }
}
