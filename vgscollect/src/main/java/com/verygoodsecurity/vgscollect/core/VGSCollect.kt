package com.verygoodsecurity.vgscollect.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import com.verygoodsecurity.vgscollect.app.BaseTransmitActivity
import com.verygoodsecurity.vgscollect.core.api.*
import com.verygoodsecurity.vgscollect.core.model.*
import com.verygoodsecurity.vgscollect.core.model.VGSHashMapWrapper
import com.verygoodsecurity.vgscollect.core.model.network.VGSError
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import com.verygoodsecurity.vgscollect.core.model.state.FieldState
import com.verygoodsecurity.vgscollect.core.model.state.mapToFieldState
import com.verygoodsecurity.vgscollect.core.storage.*
import com.verygoodsecurity.vgscollect.core.storage.content.file.StorageErrorListener
import com.verygoodsecurity.vgscollect.core.storage.content.file.VGSFileProvider
import com.verygoodsecurity.vgscollect.core.storage.content.file.TemporaryFileStorage
import com.verygoodsecurity.vgscollect.core.storage.external.DependencyReceiver
import com.verygoodsecurity.vgscollect.core.storage.external.ExternalDependencyDispatcher
import com.verygoodsecurity.vgscollect.util.Logger
import com.verygoodsecurity.vgscollect.util.deepMerge
import com.verygoodsecurity.vgscollect.util.mapToMap
import com.verygoodsecurity.vgscollect.util.mapUsefulPayloads
import com.verygoodsecurity.vgscollect.view.AccessibilityStatePreparer
import com.verygoodsecurity.vgscollect.view.InputFieldView


/**
 * VGS Collect allows you to securely collect data and files from your users without having
 * to have them pass through your systems.
 * Entry-point to the Collect SDK.
 *
 * @since 1.0.0
 */
class VGSCollect {

    private val externalDependencyDispatcher: ExternalDependencyDispatcher

    private lateinit var client: ApiClient

    private lateinit var storage:InternalStorage
    private var storageErrorListener:StorageErrorListener

    private val responseListeners = mutableListOf<VgsCollectResponseListener>()

    private var currentTask:AsyncTask<Payload, Void, VGSResponse>? = null

    private val baseURL:String
    private val context: Context

    private val isURLValid:Boolean

    constructor(
        /** Activity context */
        context: Context,

        /** Unique Vault id */
        id: String,

        /** Type of Vault */
        environment: Environment = Environment.SANDBOX
    ) {
        this.context = context

        baseURL = id.setupURL(environment.rawValue)
        isURLValid = baseURL.isURLValid()
        initializeCollect(baseURL)
    }

    constructor(
        /** Activity context */
        context: Context,

        /** Unique Vault id */
        id: String,

        /** Type of Vault */
        environment: String
    ) {
        this.context = context

        baseURL = id.setupURL(environment)
        isURLValid = baseURL.isURLValid()
        initializeCollect(baseURL)

    }

    private fun initializeCollect(baseURL: String) {
        client = OkHttpClient.newInstance(context, baseURL)
        storage = InternalStorage(context, storageErrorListener)
    }


    init {
        externalDependencyDispatcher = DependencyReceiver()

        storageErrorListener = object : StorageErrorListener {
            override fun onStorageError(error: VGSError) {
                notifyErrorResponse(error)
            }
        }
    }

    /**
     * Adds a listener to the list of those whose methods are called whenever the VGSCollect receive response from Server.
     *
     * @param onResponseListener Interface definition for a receiving callback.
     */
    fun addOnResponseListeners(onResponseListener:VgsCollectResponseListener?) {
        onResponseListener?.let {
            responseListeners.add(it)
        }
    }

    /**
     * Allows VGS secure fields to interact with [VGSCollect] and collect data from this source.
     *
     * @param view base class for VGS secure fields.
     */
    fun bindView(view: InputFieldView?) {
        if(view is AccessibilityStatePreparer) {
            externalDependencyDispatcher.addDependencyListener(view.getFieldName(), view.getDependencyListener())
        }
        storage.performSubscription(view)
    }

    /**
     * This method adds a listener whose methods are called whenever VGS secure fields state changes.
     *
     * @param fieldStateListener listener which will notify about changes inside input fields.
     */
    fun addOnFieldStateChangeListener(fieldStateListener : OnFieldStateChangeListener?) {
        storage.attachStateChangeListener(fieldStateListener)
    }

    /**
     * Clear all information collected before by VGSCollect.
     * Preferably call it inside onDestroy system's callback.
     */
    fun onDestroy() {
        currentTask?.cancel(true)
        responseListeners.clear()
        storage.clear()
    }

    /**
     * Returns the states of all fields bound before to VGSCollect.
     *
     * @return the list of all input fields states, that were bound before.
     */
    fun getAllStates(): List<FieldState> {
        return storage.getFieldsStates().map { it.mapToFieldState() }
    }

    /**
     * This method executes and send data on VGS Server. It could be useful if you want to handle
     * multithreading by yourself.
     * Do not use this method on the UI thread as this may crash.
     *
     * @param path path for a request
     * @param method HTTP method
     */
    fun submit(path:String
               , method:HTTPMethod = HTTPMethod.POST
    ) {
        val request = VGSRequest.VGSRequestBuilder()
            .setPath(path)
            .setMethod(method)
            .build()

        if(checkInternetPermission() && isUrlValid() && validateFields() && validateFiles()) {
            doRequest(request)
        }
    }


    /**
     * This method executes and send data on VGS Server. It could be useful if you want to handle
     * multithreading by yourself.
     * Do not use this method on the UI thread as this may crash.
     *
     * @param request data class with attributes for submit.
     */
    fun submit(request: VGSRequest) {
        if(isUrlValid() && checkInternetPermission()) {
            if(!request.fieldsIgnore && !validateFields()) {
                return
            }
            if(!request.fileIgnore&& !validateFiles()) {
                return
            }
            doRequest(request)
        }
    }

    /**
     * This method executes and send data on VGS Server.
     *
     * @param path path for a request
     * @param method HTTP method
     */
    fun asyncSubmit(path:String
                    , method:HTTPMethod
    ) {
        val request = VGSRequest.VGSRequestBuilder()
            .setPath(path)
            .setMethod(method)
            .build()

        if(checkInternetPermission() && isUrlValid() && validateFields() && validateFiles()) {
            doAsyncRequest(request)
        }
    }

    /**
     * This method executes and send data on VGS Server.
     *
     * @param request data class with attributes for submit
     */
    fun asyncSubmit(request: VGSRequest) {
        if(isUrlValid() && checkInternetPermission()) {
            if(!request.fieldsIgnore && !validateFields()) {
                return
            }
            if(!request.fileIgnore&& !validateFiles()) {
                return
            }
            doAsyncRequest(request)
        }
    }

    private fun checkInternetPermission():Boolean {
        return with(ContextCompat.checkSelfPermission(context, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_DENIED) {
            if (this.not()) {
                notifyErrorResponse(VGSError.NO_INTERNET_PERMISSIONS)
            }
            this
        }
    }

    private fun isUrlValid():Boolean {
        return with(isURLValid) {
            if (this.not()) {
                notifyErrorResponse(VGSError.URL_NOT_VALID)
            }
            this
        }
    }

    private fun validateFiles():Boolean {
        var isValid = true

        storage.getAttachedFiles().forEach {
            if(it.size > storage.getFileSizeLimit()) {
                notifyErrorResponse(VGSError.FILE_SIZE_OVER_LIMIT, it.name)

                isValid = false
                return@forEach
            }
        }

        return isValid
    }

    private fun validateFields():Boolean {
        var isValid = true

        storage.getFieldsStorage().getItems().forEach {
            if(it.isValid.not()) {
                notifyErrorResponse(VGSError.INPUT_DATA_NOT_VALID, it.fieldName)
                isValid = false
                return@forEach
            }
        }
        return isValid
    }

    private fun notifyErrorResponse(error: VGSError, vararg params:String?) {
        val message = if(params.isEmpty()) {
            context.getString(error.messageResId)
        } else {
            String.format(
                context.getString(error.messageResId),
                *params
            )
        }
        responseListeners.forEach {
            it.onResponse(VGSResponse.ErrorResponse(message, error.code))
        }
        Logger.e(VGSCollect::class.java, message)
    }

    private fun doRequest(
        request: VGSRequest
    ) {
        val requestBodyMap = request.customData.run {
            val map = HashMap<String, Any>()
            val customData = client.getTemporaryStorage().getCustomData()
            val newDynamicData = this.mapToMap()
            val newStaticData = customData.mapToMap()
            val mergedMap = newStaticData.deepMerge(newDynamicData)

            map.putAll(mergedMap)
            map
        }

        val data = storage.getAssociatedList(request.fieldsIgnore, request.fileIgnore)
            .mapUsefulPayloads(requestBodyMap)
        val r = client.call(request.path, request.method, request.customHeader, data)
        responseListeners.forEach { it.onResponse(r) }
    }

    private fun doAsyncRequest(
        request: VGSRequest
    ) {
        if(currentTask?.isCancelled == false) {
            currentTask?.cancel(true)
        }
        currentTask = doAsync(responseListeners) {
            it?.run {
                val requestBodyMap = request.customData.run {
                    val map = HashMap<String, Any>()
                    val customData = client.getTemporaryStorage().getCustomData()
                    val newDynamicData = this.mapToMap()
                    val newStaticData = customData.mapToMap()
                    val mergedMap = newStaticData.deepMerge(newDynamicData)

                    map.putAll(mergedMap)
                    map
                }

                val data = storage.getAssociatedList(request.fieldsIgnore, request.fileIgnore).mapUsefulPayloads(requestBodyMap)
                val r = client.call(this.path, this.method, this.headers, data)
                r
            } ?: VGSResponse.ErrorResponse()
        }

        val p = Payload(request.path, request.method, request.customHeader, request.customData)
        currentTask!!.execute(p)
    }

    /**
     * Called when an activity you launched exits,
     * giving you the requestCode you started it with, the resultCode is returned,
     * and any additional data for VGSCollect.
     * Preferably call it inside onActivityResult system's callback.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK) {
            val map: VGSHashMapWrapper<String, Any?>? = data?.extras?.getParcelable(
                BaseTransmitActivity.RESULT_DATA
            )

            if(requestCode == TemporaryFileStorage.REQUEST_CODE) {
                map?.run {
                    storage.getFileStorage().dispatch(mapOf())
                }
            } else {
                map?.run {
                    externalDependencyDispatcher.dispatch(mapOf())
                }
            }
        }
    }

    /**
     * It collects headers that will be sent to the server.
     * This is static headers that are stored and attach for all requests until @resetCustomHeaders method will be called.
     *
     * @param headers The headers to save for request.
     */
    fun setCustomHeaders(headers: Map<String, String>?) {
        client.getTemporaryStorage().setCustomHeaders(headers)
    }

    /**
     * Reset all static headers which added before.
     * This method has no impact on all custom data that were added with [VGSRequest]
     */
    fun resetCustomHeaders() {
        client.getTemporaryStorage().resetCustomHeaders()
    }

    /**
     * It collect custom data which will be send to server.
     * This is static custom data that are stored and attach for all requests until resetCustomData method will be called.
     *
     * @param data The Map to save for request.
     */
    fun setCustomData(data: Map<String, Any>?) {
        client.getTemporaryStorage().setCustomData(data)
    }

    /**
     * Reset all static custom data which added before.
     * This method has no impact on all custom data that were added with [VGSRequest]
     */
    fun resetCustomData() {
        client.getTemporaryStorage().resetCustomData()
    }

    /**
     * Return instance for managing attached files to request.
     *
     * @return [VGSFileProvider] instance
     */
    fun getFileProvider(): VGSFileProvider {
        return storage.getFileProvider()
    }

    @VisibleForTesting
    internal fun getResponseListeners(): Collection<VgsCollectResponseListener> {
        return responseListeners
    }

    @VisibleForTesting
    internal fun setStorage(store: InternalStorage) {
        storage = store
    }

    @VisibleForTesting
    internal fun setClient(c: ApiClient) {
        client = c
    }
}