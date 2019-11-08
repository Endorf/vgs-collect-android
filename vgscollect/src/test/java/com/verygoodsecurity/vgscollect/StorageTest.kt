package com.verygoodsecurity.vgscollect

import com.verygoodsecurity.vgscollect.core.model.state.FieldState
import com.verygoodsecurity.vgscollect.core.model.state.VGSFieldState
import com.verygoodsecurity.vgscollect.core.storage.DefaultStorage
import com.verygoodsecurity.vgscollect.core.storage.OnFieldStateChangeListener
import org.junit.Assert
import org.junit.Test

class StorageTest {

    @Test
    fun addItem() {
        val store = DefaultStorage()

        store.addItem(0, VGSFieldState(isFocusable = false))
        Assert.assertEquals(1, store.getStates().size)

        store.addItem(1, VGSFieldState(isFocusable = false))
        Assert.assertEquals(2, store.getStates().size)

        store.addItem(0, VGSFieldState(isFocusable = true))
        Assert.assertEquals(2, store.getStates().size)

        store.addItem(2, VGSFieldState(isFocusable = true))
        Assert.assertEquals(3, store.getStates().size)
    }

    @Test
    fun notifyUserFieldChanged() {
        var userLastUpdatedState:FieldState? = null
        val listener = object : OnFieldStateChangeListener {
            override fun onStateChange(state: FieldState) {
                userLastUpdatedState = state
            }
        }

        val store = DefaultStorage()
        store.onFieldStateChangeListener = listener

        store.addItem(0, VGSFieldState(isFocusable = false, isRequired = true, alias = "alias"))
        Assert.assertNotNull("FieldState didn't update", userLastUpdatedState)

        val viewState = VGSFieldState(isFocusable = true, isRequired = false, alias = "alias1")
        store.addItem(0, viewState)

        val isEqual = userLastUpdatedState?.hasFocus == viewState.isFocusable &&
                userLastUpdatedState?.isRequired == viewState.isRequired &&
                userLastUpdatedState?.alias == viewState.alias
        Assert.assertTrue("FieldState didn't update. User get different state", isEqual)
    }

    @Test
    fun performSubscription() {
        val store = DefaultStorage()
        val listener = store.performSubscription()

        val item = VGSFieldState()
        listener.emit(0, item)
        val cTest = store.getStates()
        Assert.assertTrue(cTest.any())
        val siTest = cTest.find { it == item }
        Assert.assertNotNull(siTest)

        val item2 = VGSFieldState()
        listener.emit(1, item2)
        val cTest2 = store.getStates()
        Assert.assertEquals(2, cTest2.size)
    }

    @Test
    fun clear() {
        val store = DefaultStorage()
        store.addItem(0, VGSFieldState())
        store.addItem(1, VGSFieldState())

        Assert.assertEquals(2, store.getStates().size)

        store.clear()
        Assert.assertEquals(0, store.getStates().size)
    }
}