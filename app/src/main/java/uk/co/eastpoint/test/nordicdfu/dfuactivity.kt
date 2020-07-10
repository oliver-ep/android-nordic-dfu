package uk.co.eastpoint.test.nordicdfu

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.content_dfuactivity.*
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import java.io.File

class dfuactivity : AppCompatActivity() {

    //region Members
    private var TAG : String  = "dfuactivity"
    private val deviceId : String = "E2:83:BD:2F:9B:57" // hard code
    private var firmwarePath : String? = null
    private var fileStreamUri : Uri? = null
    private var dfuProgress : Int = 50
    private var dfuStarted = false
    private var fileSize : Long = 0

    private val ENABLE_BT_REQ = 0;
    private val SELECT_FILE_REQ = 1;
    //endregion

    //region Special member: DfuProgressListener - initialised with adapter
    /*
        DfuProgressListener implementation with inline override -
        to tie up events with UI elements available for this activity.
     */
    private val dfuProgressListener : DfuProgressListener = object : DfuProgressListenerAdapter() {
        private val TAG = "DfuProgressListener"
        override fun onProgressChanged(
            deviceAddress: String,
            percent: Int,
            speed: Float,
            avgSpeed: Float,
            currentPart: Int,
            partsTotal: Int
        ) {
            Log.i(TAG,"onProgresseChanged - Enter ")

            updateProgressBar(percent)
            if (partsTotal > 1)
            {
                txtProgress.text = "$percent % for file part $currentPart / $partsTotal"
            }
            Log.i(TAG,"onProgresseChanged - Exit ")
        }

        override fun onDeviceDisconnecting(deviceAddress: String?) {
            Log.i(TAG,"onDeviceDisconnecting - Enter ")
//            txtStatus.text = deviceAddress ?: "Disconnected from $deviceAddress"
            txtStatus.setText(R.string.dfu_status_disconnecting)
            Log.i(TAG,"onDeviceDisconnecting - Exit ")
        }

        override fun onDeviceDisconnected(deviceAddress: String) {
            Log.i(TAG,"onDeviceDisconnected - Enter ")
            txtStatus.text = "Disconnected from device"
//            TODO("Not yet implemented")
            Log.i(TAG,"onDeviceDisconnected - Exit ")
        }

        override fun onDeviceConnected(deviceAddress: String) {
            Log.i(TAG,"onDeviceConnected - Enter ")
            txtStatus.text = "Connected to device"
//            TODO("Not yet implemented")
            Log.i(TAG,"onDeviceConnected - Exit ")
        }

        override fun onDfuProcessStarting(deviceAddress: String) {
            Log.i(TAG,"onDfuProcessStarting - Enter ")
            txtStatus.setText(R.string.dfu_status_starting)
//            TODO("Not yet implemented")
            Log.i(TAG,"onDfuProcessStarting - Exit ")
        }

        override fun onDfuAborted(deviceAddress: String) {
            Log.i(TAG,"onDfuAborted - Enter ")
            txtStatus.setText(R.string.dfu_status_aborted)
            dfuStarted = false
            // TODO: deal with notification cancelling
            // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
//            Handler().postDelayed({
//                onUploadCanceled()
//
//                // if this activity is still open and upload process was completed, cancel the notification
//                val manager =
//                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//                manager.cancel(DfuService.NOTIFICATION_ID)
//            }, 200)

            Log.i(TAG,"onDfuAborted - Exit ")
        }

        override fun onEnablingDfuMode(deviceAddress: String) {
            Log.i(TAG,"onEnablingDfuMode - Enter ")
            txtStatus.setText(R.string.dfu_status_switching_to_dfu)
            Log.i(TAG,"onEnablingDfuMode - Exit ")
        }

        override fun onDfuCompleted(deviceAddress: String) {
            Log.i(TAG,"onDfuCompleted - Enter ")
            txtStatus.setText(R.string.dfu_status_completed)
            dfuStarted = false
            // TODO: Add resume feature
//            if (resumed) {
//                // let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
//                Handler().postDelayed({
//                    onTransferCompleted()
//
//                    // if this activity is still open and upload process was completed, cancel the notification
//                    val manager =
//                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//                    manager.cancel(DfuService.NOTIFICATION_ID)
//                }, 200)
//            } else {
//                // Save that the DFU process has finished
//                dfuCompleted = true
//            }

            Log.i(TAG,"onDfuCompleted - Exit ")
        }

        override fun onFirmwareValidating(deviceAddress: String) {
            Log.i(TAG,"onFirmwareValidating - Enter ")
            txtStatus.setText(R.string.dfu_status_validating)
            Log.i(TAG,"onFirmwareValidating - Exit ")
        }

        override fun onDfuProcessStarted(deviceAddress: String) {
            Log.i(TAG,"onDfuProcessStarted - Enter ")
            txtStatus.setText(R.string.dfu_status_starting)
            Log.i(TAG,"onDfuProcessStarted - Exit ")
        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String?) {
            Log.i(TAG,"onError - Enter ")
            txtStatus.setText(R.string.dfu_status_error)
//            TODO("Not yet implemented")
            dfuStarted = false
            Log.i(TAG,"onError - Exit ")
        }

        override fun onDeviceConnecting(deviceAddress: String) {
            Log.i(TAG,"onDeviceConnecting - Enter ")
            txtStatus.setText(R.string.dfu_status_connecting)
            Log.i(TAG,"onDeviceConnecting - Exit ")
        }
    }

    //endregion

    //region Activity Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dfuactivity)
        setSupportActionBar(findViewById(R.id.toolbar))

        // Update UI based on data from intent
        updateProgressBar(progressPercentage = dfuProgress) // just to test UI, defualt is 50.

        // Seek Bluetooth permission
        isBLESupported() // this methods ends the app if BLE is not supported on the device
        if (isBLEEnabled() == false) {
            showBLEDialog() // seek permission
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(this)
        }

        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener)

        // Immediately ask for file to load, firmware file should be in the downloads folder
        val intent = Intent(Intent.ACTION_GET_CONTENT)
            .setType("application/zip")
            .addCategory(Intent.CATEGORY_OPENABLE)

        if(intent.resolveActivity(getPackageManager()) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, SELECT_FILE_REQ)
        } else {
            Log.i(TAG,"File selection app doesn't exist. Go download one.")
        }

    }

    /**
     * Expect to enter here after file selection - ONLY using zip files at the moment
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if(resultCode != RESULT_OK) return

        if (requestCode == SELECT_FILE_REQ)
        {
            val uri : Uri? = resultData?.getData()

            if(uri?.scheme == "file")
            {
                firmwarePath = uri?.getPath()
                val file : File? = File(firmwarePath)
                fileSize = file?.length() ?: 0
            } else if (uri?.scheme == "content") {
                fileStreamUri = uri

                resultData?.data?.let { returnUri ->
                    contentResolver.query(returnUri, null, null, null, null)
                }?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        Log.i(TAG, "Display name : $displayName")
                        val sizeIndex  = cursor.getColumnIndex(OpenableColumns.SIZE)
                        fileSize = if (cursor.isNull(sizeIndex) == false) {
                            // Technically the column stores an int, but cursor.getString()
                            // will do the conversion automatically.
                            cursor.getLong(sizeIndex)

                        } else {
                            0
                        }
                        Log.i(TAG, "Size : ${fileSize.toString()}")

                    } else {
                        Log.i(TAG, "No item found")
                    }
                }
            }

        } else {
            Log.e(TAG,"Unknown request code received on Activity $requestCode")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        DfuServiceListenerHelper.unregisterProgressListener(this, dfuProgressListener)
    }

    //endregion

    //region BLE related
    open fun isBLESupported(): Unit {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(TAG,"BLE not supported")
            finish()
        }
    }

    private fun isBLEEnabled(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val result = adapter != null && adapter.isEnabled
        Log.i(TAG,"isBLEEnabled: $result")
        return result
    }

    private fun showBLEDialog() {
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(
            enableIntent,
            ENABLE_BT_REQ
        )
    }

    //endregion

    //region Methods

    fun onStartUpload(view:View) {
        Log.i(TAG, "Start Upload clicked")

        if (dfuStarted) {
            Log.i(TAG,"DFU already in process. Doing nothing")
            return
        } else {
            dfuStarted = true
            Log.i(TAG, "Initiating upload")
        }

        // get device ID
        if(deviceId.isNullOrEmpty() ) {
            Log.i(TAG,"deviceId was not set. For this sample it should be hard coded.")
            return }

        // get file path
        if(fileStreamUri == null)
        {
            Log.i(TAG, "FileStreamUri must be set for this version")
            return
        }

        // initiate upload process
        Log.i(TAG, "Starting DFU process")

        // scan and get device
        val starter : DfuServiceInitiator = DfuServiceInitiator(deviceId!!)
            .setDeviceName("Nordic_")
            .setKeepBond(false)
            .setForceDfu(false)
            .setPacketsReceiptNotificationsEnabled(false)
            .setPacketsReceiptNotificationsValue(12)
            .setPrepareDataObjectDelay(400)
            .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)

//        val fileStreamUri : Uri = Uri.parse("content://com.android.providers.downloads.documents/document/msf%3A296")
        Log.i(TAG,"Setting up Init packet for distribution firmware format")
        starter.setZip(fileStreamUri!!) // can use filpath if prefered

        Log.i(TAG,"Starting DFU process")
        // -- NOTE: Make sure the app has BLE and Location permission
        starter.start(this, DfuService::class.java)
        Log.i(TAG,"DFU process started successfully")

    }

    /**
     * Update progress bar UI.
     * Expected input is 0 - 100 Integer values representing %
     */
    fun updateProgressBar(progressPercentage: Int) {
        progressBarDfu.progress = progressPercentage
        txtProgress.text = "$progressPercentage %"
    }

    //endregion

}