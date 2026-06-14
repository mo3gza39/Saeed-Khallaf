package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.theme.MyApplicationTheme

class KeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private lateinit var viewModel: KeyboardViewModel

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Instantiate our shared ViewModel with application context
        viewModel = KeyboardViewModel(application)
        viewModel.isImeMode.value = true

        // Route real-time keyboard events from composables directly into active InputConnection
        viewModel.imeListener = object : ImeListener {
            override fun onCommitText(text: String) {
                if (text == "SAVE") {
                    // Send keyboard Done/Enter action, or a real new line fallback
                    val editorInfo = currentInputEditorInfo
                    val connection = currentInputConnection
                    if (editorInfo != null && connection != null) {
                        val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
                        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                            connection.performEditorAction(action)
                        } else {
                            connection.commitText("\n", 1)
                        }
                    } else {
                        connection?.commitText("\n", 1)
                    }
                } else {
                    currentInputConnection?.commitText(text, 1)
                }
            }

            override fun onDeleteBackward() {
                val connection = currentInputConnection ?: return
                val selectedText = connection.getSelectedText(0)
                if (selectedText.isNullOrEmpty()) {
                    connection.deleteSurroundingText(1, 0)
                } else {
                    connection.commitText("", 1)
                }
            }

            override fun onReplaceLastWord(suggestion: String) {
                val connection = currentInputConnection ?: return
                // Retrieve the string buffer before cursor to locate the contiguous typed prefix
                val textBeforeCursor = connection.getTextBeforeCursor(50, 0) ?: ""
                val lastWord = textBeforeCursor.split(" ", "\n").lastOrNull() ?: ""
                if (lastWord.isNotEmpty()) {
                    connection.deleteSurroundingText(lastWord.length, 0)
                }
                connection.commitText(suggestion + " ", 1)
            }
        }
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this)
        
        // Wire up critical Lifecycle owners to avoid crash exceptions inside ComposeView in services
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeViewModelStoreOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeView.setContent {
            MyApplicationTheme {
                MainGlassKeyboard(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        return composeView
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        // Reset typed tracking buffer when starting a fresh input focus
        viewModel.onKeyPress("CLR", composeViewPlaceholder)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        viewModel.onKeyPress("CLR", composeViewPlaceholder)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    // Lazy placeholder view to satisfy click event references in VM haptics
    private val composeViewPlaceholder by lazy { ComposeView(this) }
}
