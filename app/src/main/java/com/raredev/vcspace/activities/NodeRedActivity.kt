/*
 * This file is part of Visual Code Space.
 *
 * Visual Code Space is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Visual Code Space is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Visual Code Space.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.raredev.vcspace.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.SizeUtils
import com.raredev.vcspace.databinding.ActivityTerminalBinding
import com.raredev.vcspace.dialogs.ProgressDialogBuilder
import com.raredev.vcspace.ui.virtualkeys.*
import com.raredev.vcspace.utils.Logger
import com.raredev.vcspace.utils.TerminalPythonCommands
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException

class NodeRedActivity : BaseActivity(), TerminalViewClient, TerminalSessionClient {
  private val logger = Logger.newInstance("NodeRedActivity")
  private var binding: ActivityTerminalBinding? = null
  private var session: TerminalSession? = null
  private lateinit var terminal: TerminalView
  private var listener: KeyListener? = null
  override fun getLayout(): View {
    binding = ActivityTerminalBinding.inflate(layoutInflater)
    return binding!!.root
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    super.onCreate(savedInstanceState)
    setupTerminalView()

    if (intent.getBooleanExtra(KEY_CONTAINS_PYTHON_FILE, false)) {
      val pyFilePath = intent.getStringExtra(KEY_PYTHON_FILE_PATH)
      if (pyFilePath != null) {
        val command = TerminalPythonCommands.getInterpreterCommand(this, pyFilePath)
        val dialog = ProgressDialogBuilder(this)
          .setCancelable(false)
          .setTitle("Compiling...")
          .setMessage("Please wait")
          .create()
        dialog.show()

        CoroutineScope(Dispatchers.Main).launch {
          terminal.mTermSession.write("$command\r")
          dialog.dismiss()
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    showSoftInput()
    setTerminalCursorBlinkingState(true)
  }

  override fun onStop() {
    super.onStop()
    setTerminalCursorBlinkingState(false)
  }

  override fun onDestroy() {
    super.onDestroy()
    binding = null
  }

  override val navigationBarColor: Int
    get() = ContextCompat.getColor(this, android.R.color.black)
  override val statusBarColor: Int
    get() = ContextCompat.getColor(this, android.R.color.black)

  private fun setupTerminalView() {
    terminal = TerminalView(this, null)
    terminal.setTerminalViewClient(this)
    terminal.attachSession(createSession())
    terminal.keepScreenOn = true
    terminal.setTextSize(SizeUtils.dp2px(14f))
    val params = LinearLayout.LayoutParams(-1, 0)
    params.weight = 1f
    binding!!.root.addView(terminal, 0, params)
    try {
      binding!!.virtualKeys.virtualKeysViewClient = keyListener
      binding!!.virtualKeys.reload(
        VirtualKeysInfo(VIRTUAL_KEYS, "", VirtualKeysConstants.CONTROL_CHARS_ALIASES)
      )
    } catch (e: JSONException) {
      logger.e("Unable to parse terminal virtual keys json data", e)
    }
  }

  private val keyListener: KeyListener
    get() = if (listener == null) KeyListener(terminal).also {
      listener = it
    } else listener!!

  private fun createSession(): TerminalSession {
    session = TerminalSession(
      "/system/bin/sh",
      workingDirectory, arrayOf(), arrayOf(),
      TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
      this
    )
    return session!!
  }

  private val workingDirectory: String
    get() {
      val extras = intent.extras
      if (extras != null && extras.containsKey(KEY_WORKING_DIRECTORY)) {
        var directory = extras.getString(KEY_WORKING_DIRECTORY, null)
        if (directory == null || directory.trim { it <= ' ' }.isEmpty()) {
          directory = PathUtils.getRootPathExternalFirst()
        }
        return directory
      }
      return PathUtils.getRootPathExternalFirst()
    }

  override fun onTextChanged(changedSession: TerminalSession) {
    terminal.onScreenUpdated()
  }

  override fun onTitleChanged(changedSession: TerminalSession) {}
  override fun onSessionFinished(finishedSession: TerminalSession) {
    finish()
  }

  override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
    ClipboardUtils.copyText("VCSpace Terminal", text)
  }

  override fun onPasteTextFromClipboard(session: TerminalSession) {
    val clip = ClipboardUtils.getText().toString()
    if (clip.trim { it <= ' ' }.isNotEmpty() && terminal.mEmulator != null) {
      terminal.mEmulator.paste(clip)
    }
  }

  override fun onBell(session: TerminalSession) {}
  override fun onColorsChanged(session: TerminalSession) {}
  override fun onTerminalCursorStateChange(state: Boolean) {}
  override fun getTerminalCursorStyle(): Int {
    return if (intent.getBooleanExtra(KEY_CONTAINS_PYTHON_FILE, false)) {
      TerminalEmulator.TERMINAL_CURSOR_STYLE_UNDERLINE
    } else {
      TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE
    }
  }

  override fun logError(tag: String, message: String) {
    logger.e(message)
  }

  override fun logWarn(tag: String, message: String) {
    logger.w(message)
  }

  override fun logInfo(tag: String, message: String) {
    logger.i(message)
  }

  override fun logDebug(tag: String, message: String) {
    logger.d(message)
  }

  override fun logVerbose(tag: String, message: String) {
    logger.v(message)
  }

  override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
    logger.w(Log.getStackTraceString(e))
  }

  override fun logStackTrace(tag: String, e: Exception) {
    logger.w(Log.getStackTraceString(e))
  }

  override fun onScale(scale: Float): Float {
    return SizeUtils.dp2px(14f).toFloat()
  }

  override fun onSingleTapUp(e: MotionEvent) {
    showSoftInput()
  }

  override fun shouldBackButtonBeMappedToEscape(): Boolean {
    return false
  }

  override fun shouldEnforceCharBasedInput(): Boolean {
    return true
  }

  override fun shouldUseCtrlSpaceWorkaround(): Boolean {
    return false
  }

  override fun isTerminalViewSelected(): Boolean {
    return true
  }

  override fun copyModeChanged(copyMode: Boolean) {}
  override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
    if (keyCode == KeyEvent.KEYCODE_ENTER && !session.isRunning) {
      finish()
      return true
    }
    return false
  }

  override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean {
    return false
  }

  override fun onLongPress(event: MotionEvent): Boolean {
    return false
  }

  override fun readControlKey(): Boolean {
    val state = binding!!.virtualKeys.readSpecialButton(SpecialButton.CTRL, true)
    return state != null && state
  }

  override fun readAltKey(): Boolean {
    val state = binding!!.virtualKeys.readSpecialButton(SpecialButton.ALT, true)
    return state != null && state
  }

  override fun readShiftKey(): Boolean {
    return false
  }

  override fun readFnKey(): Boolean {
    return false
  }

  override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean {
    return false
  }

  override fun onEmulatorSet() {
    setTerminalCursorBlinkingState(true)
    if (session != null) {
      binding
        ?.root
        ?.setBackgroundColor(
          session!!.emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]
        )
    }
  }

  private fun setTerminalCursorBlinkingState(start: Boolean) {
    if (terminal.mEmulator != null) {
      terminal.setTerminalCursorBlinkerState(start, true)
    }
  }

  private fun showSoftInput() {
    terminal.requestFocus()
    KeyboardUtils.showSoftInput(terminal)
  }

  private class KeyListener(private val terminal: TerminalView?) : VirtualKeysView.IVirtualKeysView {
    override fun onVirtualKeyButtonClick(
      view: View,
      buttonInfo: VirtualKeyButton,
      button: Button
    ) {
      if (terminal == null) {
        return
      }
      if (buttonInfo.isMacro) {
        val keys = buttonInfo.key.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
          .toTypedArray()
        var ctrlDown = false
        var altDown = false
        var shiftDown = false
        var fnDown = false
        for (key in keys) {
          if (SpecialButton.CTRL.key == key) {
            ctrlDown = true
          } else if (SpecialButton.ALT.key == key) {
            altDown = true
          } else if (SpecialButton.SHIFT.key == key) {
            shiftDown = true
          } else if (SpecialButton.FN.key == key) {
            fnDown = true
          } else {
            onTerminalExtraKeyButtonClick(key, ctrlDown, altDown, shiftDown, fnDown)
            ctrlDown = false
            altDown = false
            shiftDown = false
            fnDown = false
          }
        }
      } else {
        onTerminalExtraKeyButtonClick(
          buttonInfo.key,
          ctrlDown = false,
          altDown = false,
          shiftDown = false,
          fnDown = false
        )
      }
    }

    private fun onTerminalExtraKeyButtonClick(
      key: String, ctrlDown: Boolean, altDown: Boolean, shiftDown: Boolean, fnDown: Boolean
    ) {
      if (VirtualKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS.containsKey(key)) {
        val keyCode = VirtualKeysConstants.PRIMARY_KEY_CODES_FOR_STRINGS[key] ?: return
        var metaState = 0
        if (ctrlDown) {
          metaState = metaState or (KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON)
        }
        if (altDown) {
          metaState = metaState or (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON)
        }
        if (shiftDown) {
          metaState = metaState or (KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON)
        }
        if (fnDown) {
          metaState = metaState or KeyEvent.META_FUNCTION_ON
        }
        val keyEvent = KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, metaState)
        terminal!!.onKeyDown(keyCode, keyEvent)
      } else {
        // not a control char
        var off = 0
        while (off < key.length) {
          val codePoint = key.codePointAt(off)
          terminal!!.inputCodePoint(codePoint, ctrlDown, altDown)
          off += Character.charCount(codePoint)
        }
      }
    }

    override fun performVirtualKeyButtonHapticFeedback(
      view: View, buttonInfo: VirtualKeyButton, button: Button
    ): Boolean {
      // No need to handle this
      // VirtualKeysView will take care of performing haptic feedback
      return false
    }
  }

  companion object {
    const val KEY_WORKING_DIRECTORY = "terminal_workingDirectory"
    const val KEY_CONTAINS_PYTHON_FILE = "contains_py_file"
    const val KEY_PYTHON_FILE_PATH = "py_file_path"

    fun startTerminalWithDir(context: Context, path: String?) {
      val it = Intent(context, NodeRedActivity::class.java)
      it.putExtra(KEY_WORKING_DIRECTORY, path)
      context.startActivity(it)
    }

    const val VIRTUAL_KEYS = ("["
      + "\n  ["
      + "\n    \"ESC\","
      + "\n    {"
      + "\n      \"key\": \"/\","
      + "\n      \"popup\": \"\\\\\""
      + "\n    },"
      + "\n    {"
      + "\n      \"key\": \"-\","
      + "\n      \"popup\": \"|\""
      + "\n    },"
      + "\n    \"HOME\","
      + "\n    \"UP\","
      + "\n    \"END\","
      + "\n    \"PGUP\""
      + "\n  ],"
      + "\n  ["
      + "\n    \"TAB\","
      + "\n    \"CTRL\","
      + "\n    \"ALT\","
      + "\n    \"LEFT\","
      + "\n    \"DOWN\","
      + "\n    \"RIGHT\","
      + "\n    \"PGDN\""
      + "\n  ]"
      + "\n]")
  }
}