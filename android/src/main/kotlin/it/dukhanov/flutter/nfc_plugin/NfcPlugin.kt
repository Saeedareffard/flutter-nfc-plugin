package it.dukhanov.flutter.nfc_plugin

import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.annotation.TargetApi
import android.app.Activity
import android.util.Log
import android.os.Build
import android.nfc.*
import android.content.Context
import androidx.annotation.RequiresApi
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.EventChannel.EventSink

const val METHOD_GET_NFC_STATE = "getNfcState"
const val METHOD_GET_NFC_STARTED_WITH = "getNfcStartedWith"

@RequiresApi(Build.VERSION_CODES.KITKAT)
const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A + NfcAdapter.FLAG_READER_NFC_B + NfcAdapter.FLAG_READER_NFC_V
const val PLUGIN_TAG = "FlutterNfcPlugin"
const val CHANNEL="nfc_plugin_method_channel";
@RequiresApi(Build.VERSION_CODES.KITKAT)
class NfcPlugin : FlutterPlugin, MethodCallHandler,ActivityAware,EventChannel.StreamHandler, NfcAdapter.ReaderCallback  {
	private lateinit var channel : MethodChannel

	private lateinit var eventchannel : EventChannel
	private  var  activity : Activity?=null
	private var eventSink: EventSink? = null
	private var nfcAdapter: NfcAdapter? = null
	private var nfcMessageStartedWith: Map<String, Any?>? = null

	init {
		val nfcManager = activity?.getSystemService(Context.NFC_SERVICE) as? NfcManager
		nfcAdapter = nfcManager?.defaultAdapter
	}
//	companion object {
//		@JvmStatic
//		fun registerWith() {
//			Log.d(PLUGIN_TAG, "call: registerWith")
//			val instance = NfcPlugin()
//			instance.checkIfStartedWithNfc()
//
//			val methodChannel = MethodChannel(registrar.messenger(), "nfc_plugin_method_channel")
//			val eventChannel = EventChannel(registrar.messenger(), "nfc_plugin_event_channel")
//			eventChannel.setStreamHandler(instance)
//			methodChannel.setMethodCallHandler(instance)
//		}
//	}
/** @TargetApi(Build.VERSION_CODES.KITKAT)
class NfcPlugin(registrar: Registrar) : MethodCallHandler, EventChannel.StreamHandler, NfcAdapter.ReaderCallback {

	private val activity = registrar.activity()
	private var eventSink: EventSink? = null
	private var nfcAdapter: NfcAdapter? = null
	private var nfcMessageStartedWith: Map<String, Any?>? = null

	companion object {
		@JvmStatic
		fun registerWith(registrar: Registrar) {
			Log.d(PLUGIN_TAG, "call: registerWith")
			val instance = NfcPlugin(registrar)
			instance.checkIfStartedWithNfc()

			val methodChannel = MethodChannel(registrar.messenger(), "nfc_plugin_method_channel")
			val eventChannel = EventChannel(registrar.messenger(), "nfc_plugin_event_channel")
			eventChannel.setStreamHandler(instance)
			methodChannel.setMethodCallHandler(instance)
		}
	}

	init {
		val nfcManager = activity?.getSystemService(Context.NFC_SERVICE) as? NfcManager
		nfcAdapter = nfcManager?.defaultAdapter
	}
**/
	override fun onMethodCall(call: MethodCall, result: Result) {
		Log.i(PLUGIN_TAG, "call: onMethodCall: " + call.method)
		when (call.method) {
			METHOD_GET_NFC_STATE -> {
				val state = getNfcState(nfcAdapter)
				result.success(state)
			}
			METHOD_GET_NFC_STARTED_WITH -> {
				result.success(nfcMessageStartedWith)
			}
			else -> {
				result.notImplemented()
			}
		}
	}

	override fun onListen(arguments: Any?, eventSink: EventSink?) {
		Log.i(PLUGIN_TAG, "call: onListen")
		if (this.eventSink != null) {
			Log.w(PLUGIN_TAG, "onListen NFC. NFC listener has been already registered!")
			return
		}
		this.eventSink = eventSink
		nfcReaderRestart()
	}

	override fun onCancel(arguments: Any?) {
		eventSink = null
		nfcReaderStop()
	}

	override fun onTagDiscovered(tag: Tag?) {
		val message = ndefToMap(tag)
		Log.d(PLUGIN_TAG, "callback: onTagDiscovered $message")
		activity?.runOnUiThread {
			sendNfcListenerCallback(message)
		}
	}

	private fun checkIfStartedWithNfc() {
		Log.d(PLUGIN_TAG, "call: checkIfStartedWithNfc")
		val intent = activity!!.intent
		nfcMessageStartedWith = getNfcStartedWith(intent)
	}

	private fun sendNfcListenerCallback(message: Map<String, Any?>) {
		eventSink?.success(message)
	}

	private fun nfcReaderRestart() {
		nfcReaderStop()
		nfcReaderStart()
	}

	private fun nfcReaderStart() {
		Log.d(PLUGIN_TAG, "call: nfcReaderStart")
		activity?.runOnUiThread {
			nfcAdapter?.enableReaderMode(activity, this, READER_FLAGS, null)
		}
	}

	private fun nfcReaderStop() {
		Log.d(PLUGIN_TAG, "call: nfcReaderStop")
		activity?.runOnUiThread {
			nfcAdapter?.disableReaderMode(activity)
		}
	}

	override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {

		channel = MethodChannel(binding.binaryMessenger, "nfc_plugin_method_channel")
		channel.setMethodCallHandler(this)
		eventchannel = EventChannel(binding.binaryMessenger, "nfc_plugin_event_channel")
		eventchannel.setStreamHandler(this)
	}

	override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
		channel.setMethodCallHandler(null)
	}

	override fun onAttachedToActivity(binding: ActivityPluginBinding) {
		this.activity=binding.activity;
		this.checkIfStartedWithNfc()
		val nfcManager = activity?.getSystemService(Context.NFC_SERVICE) as? NfcManager
		nfcAdapter = nfcManager?.defaultAdapter
	}

	override fun onDetachedFromActivityForConfigChanges() {
		this.activity=null;
	}

	override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
		this.activity=binding.activity;
	}

	override fun onDetachedFromActivity() {
		this.activity=null;
	}
}