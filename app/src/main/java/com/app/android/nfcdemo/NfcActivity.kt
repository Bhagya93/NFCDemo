package com.app.android.nfcdemo

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.tech.Ndef
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_nfc.*
import java.io.IOException
import java.nio.charset.Charset
import android.widget.EditText


class NfcActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    var writeNfc = false
    var ndef: Ndef? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        init()
    }

    private fun init() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        cardButtonWriteTag.text = getString(R.string.write_btn_text)
        cardButtonReadTag.text = getString(R.string.read_btn_text)

        cardButtonWriteTag.apply {
            setOnClickListener {
                hideSoftKeyboard(editTextNfc)
                writeNfc = true
                if (editTextNfc.text.toString().isNotEmpty())
                    writingToNfcTag(ndef, editTextNfc.text.toString())
                else
                    Toast.makeText(this@NfcActivity, "Please enter message!", Toast.LENGTH_LONG).show()
            }
        }

        cardButtonReadTag.apply {
            setOnClickListener {
                hideSoftKeyboard(editTextNfc)
                writeNfc = false
                readingFromNfc(ndef)
            }
        }
    }

    // Function to hide keyboard
    private fun hideSoftKeyboard(input: EditText) {
        val inputmanager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputmanager.hideSoftInputFromWindow(input.windowToken, 0)
    }

    // Function to check NFC availability and enability
    private fun checkNfcAvailability() {
        val manager = getSystemService(Context.NFC_SERVICE) as NfcManager
        val adapter = manager.defaultAdapter
        if (adapter == null) {
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setTitle("Alert !")
            builder.setMessage("Your device doesn't support NFC.")
            builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                finish()
            })
            builder.create().show()
        } else {
            if (!adapter.isEnabled) {
                val builder = AlertDialog.Builder(this)
                builder.setCancelable(false)
                builder.setTitle("NFC disable !")
                builder.setMessage("NFC is disabled.Do you want to enable it from setting?")
                builder.setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                    if (android.os.Build.VERSION.SDK_INT >= 16) {
                        startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                    } else {
                        startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                    }
                })
                builder.setNegativeButton("No", DialogInterface.OnClickListener { dialog, id ->
                    finish()
                })
                builder.create().show()
            }
        }
    }

    // This method get called when user attach nfc tag to device
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val nfcTag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (nfcTag != null) {
            ndef = Ndef.get(nfcTag)

            if (writeNfc) {
                if (editTextNfc.text.toString().isNotEmpty())
                    writingToNfcTag(ndef, editTextNfc.text.toString())
                else
                    Toast.makeText(this, "Please enter message!", Toast.LENGTH_LONG).show()
            } else {
                readingFromNfc(ndef)
            }
        }
    }

    // Function to write data entered in edittext to NFC tag
    private fun writingToNfcTag(ndef: Ndef?, message: String) {
        textViewProgress.text = getString(R.string.progress)
        if (ndef != null) {
            try {
                ndef.connect()
                val mimeNdefRecord =
                    NdefRecord.createMime("text/plain", message.toByteArray(Charset.forName("US-ASCII")))
                ndef.writeNdefMessage(NdefMessage(mimeNdefRecord))
                ndef.close()

                textViewProgress.text = getString(R.string.success)
            } catch (e: FormatException) {
                e.printStackTrace()
                textViewProgress.text = getString(R.string.error)
            } catch (e: Exception) {
                e.printStackTrace()
                textViewProgress.text = getString(R.string.error)
            }
        } else {
            textViewProgress.text = "Write - "+getString(R.string.tap_tag)
        }
    }

    // Function to read data from NFC tag
    private fun readingFromNfc(ndef: Ndef?) {
        if (ndef != null) {
            try {
                ndef?.connect()
                val ndefMessage = ndef?.ndefMessage
                val message = String(ndefMessage!!.records[0].payload)

                textViewProgress.text = message
                ndef.close()
            } catch (e: FormatException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            textViewProgress.text = "Read - "+getString(R.string.tap_tag)
        }
    }

    // Activity lifecycle onResume method
    override fun onResume() {
        super.onResume()
        checkNfcAvailability()
        val tag = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val tech = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val intentF = arrayOf<IntentFilter>(tag, ndef, tech)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
        if (nfcAdapter != null)
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentF, null)
    }

    // Activity lifecycle onPause method
    override fun onPause() {
        super.onPause()
        if (nfcAdapter != null)
            nfcAdapter?.disableForegroundDispatch(this)
    }
}
