/**
 * Copyright 2020 (Corona-Version) Schnuffiware - https://github.com/snfiware/szbsb
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.snfiware.szbsb.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Process
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import de.snfiware.szbsb.BuildConfig
import de.snfiware.szbsb.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Please read the documentation of AcmtLogger and LogHelper class below to understand on how to
// configure logging. In brief: AcmtLogger is the foundation logger class and logs like android.util.Log
// but with some really nice and handy features extra. LogHelper provides app integration by popup menu
// to control logging at runtime, extract and share logcat excerpts.
//
// These logging utilities are designed to bring minimal performance penalty to release builds but
// provide maximum comfort for debug builds. Plus the great features to get a log from released apps "in
// the wild" without the need and knowledge to use adb and usb-debugging. A simple click on a menu item
// extracts the app log as a text file from the logcat and opens the share menu.
//
val APPTAG_PLAIN = "BZ" // adjust to your needs - bear in mind android logging tags have limit in length of 23
val APP_CLS_DELIM = "#" // this delimiter is put between the apptag and the classtag
val CLS_MTH_DELIM = "." // this delimiter is put between the classtag and the methodtag
val MAXSTACKDEPTH = 10  // if this limit is exceeded we throw an assertion
//
// DEBUG_LOGGING - if true:
// - the loglevel is set to DEBUG
// - source line is appended to each log entry
// - statistics is gathered
// by default get info from build type // true // false // BuildConfig.DEBUG
val DEBUG_LOGGING = BuildConfig.DEBUG
//val DEBUG_LOGGING = true
//val DEBUG_LOGGING = false
//
// start app with this loglevel
val LOG_LEVEL = when(DEBUG_LOGGING) {true -> Log.DEBUG else -> Log.INFO}
//val LOG_LEVEL=Log.INFO
//val LOG_LEVEL=Log.DEBUG
//val LOG_LEVEL=Log.VERBOSE
//
// append to each log entry the source filename and the linenumber - enables one-click-jumps in android studio
val APPEND_SRC = DEBUG_LOGGING
//val APPEND_SRC = true
//
//val ALIGN_TO = 23       // this should put the whole log flow starting from one row
val ALIGN_TO = 0        // no alignment
val MAXLEN_CTAG = 23    // if this limit is exceeded an assertion is risen
val MAXLEN_MTAG = 23    // if this limit is exceeded an assertion is risen
val EMPTY = ""          // the empty value when no method is set (class scope logging)
//
typealias tStringStack = Stack<String>
//
// enables statistics // true // false // BuildConfig.DEBUG // DEBUG_LOGGING
val CREATE_STATISTICS = DEBUG_LOGGING
/**
 * When enabled keeps all log tags to get a list of them, how often these are used and
 * the amount of bytes written to the log.
 */
class Statistics {
    //
    class OneLogCall(var priority :Int, var bytes :Int, var time :Long =System.currentTimeMillis()) {    }
    var calls = mutableListOf<OneLogCall>()
    //
    companion object {
        val startLong =System.currentTimeMillis()
        val startDate =Date()
        // collects entries (value:StatisticsRow) clustered by logtags (key:String)
        val data = mutableMapOf<String,Statistics>()
        fun insertEntry(tag: String, priority: Int, sLogEntry: String) {
            var value = data[tag]
            if( value == null ) {
                value = Statistics()
                data[tag] = value
            }
            value.calls.add(OneLogCall(priority,sLogEntry.length))
        }
        fun getTime( call :OneLogCall ) :String {
            val sRc = (call.time - startLong).toString()
            return sRc
        }
        fun getPrio(calls : MutableList<OneLogCall>) :String {
            val group = calls.groupBy { T -> LogHelper.getLoglevel1stChar(T.priority) }
            val map = group.mapKeys { T -> "" + T.key + T.value.size }
            val sRc = map.keys.joinToString(",")
            return sRc
        }
        fun printSummary(out :StringBuilder) {
            out.append("Statistics Summary [<nbr of calls>]: in ms since ")
            out.append(LogHelper.getTimestamp(startDate, "dd.MM. HH:mm:ss"))
            out.append(" followed by total bytes written in parenthesis\n")
            for(entry in data) {
                out.append(entry.key).append("[").append(entry.value.calls.size).append("]: ")
                out.append("1st: ").append(getTime(entry.value.calls.first()))
                out.append(" ").append(getPrio(entry.value.calls))
                out.append(" last: ").append(getTime(entry.value.calls.last()))
                out.append(" (").append(entry.value.calls.sumBy { T -> T.bytes }).append(")")
                out.append('\n')
            }
        }
    }
}

/**
 * This Logger Class provides a clear und easy way to give tags to the log areas
 * you need, helps to keep track over all the tags, optionally creates statistics, provides
 * one-click-to-source-jumps and supports stack depth indention, therefore minimizing the need to
 * use debugger: the log (bug report / logcat) is sufficient for analysis. It has little overhead
 * though it can manage multithread situations: specify bSeparateStack when constructing Logger.
 *
 * On android you use logging statements like these -- Log.i("tag","my log text") --
 * This class provides a convenient way to manage the tags you use, checks for uniqueness and
 * integrity and brings semantics to the call stack indicating entering (->) or leaving (<-) a method.
 *
 *  - copy this code
 *  - put your "modified by ..." underneath the copyright (copyleft:)
 *  - change APPTAG_PLAIN to your needs
 *  - choose option 1 or 2 (see below)
 *
 *
 * >     Abstract: <APPTAG>.<CLASSTAG>.<METHODTAG>  padding stackdepth-indent   LOGTEXT
 * >             | -------- max 23 chars -----|---opt.- | |-MAXSTACKDEPTH-|   your part...
 * >                     ^          ^                   ^
 * >         APP_CLS_DELIM          CLS_MTH_DELIM       ALIGN_TO
 *
 * __Usage Option 1 - Example:__
 * >     class Main {
 * >          companion object {val CTAG = AcmtLogger("Main")}
 * >          constructor {
 * >              CTAG.enter("ctor","y")  // BZ#Main.ctor -> y
 * >              CTAG.log_("xxx")        // BZ#Main xxx
 * >              CTAG.log("wtf")         // BZ#Main.ctor wtf
 * >              CTAG.enter("getX","17") // BZ#Main.getX .-> 17
 * >              CTAG.log("wtf")         // BZ#Main.getX .wtf
 * >              CTAG.leave("bRc:$bRc")  // BZ#Main.getX .<-back2:[BZ#Main.ctor] bRc:true
 * >              CTAG.leave("z")         // BZ#Main.ctor <-back2:[class] z
 * >              CTAG.log("xxx")         // BZ#Main xxx
 * >          }
 * >     }
 *
 * If you invoke methods of another class which share the same method stack do it the same way
 * for this other class. It will share the indents. This will work perfectly for single-threaded
 * applications.
 *
 * Put the CTAG to companion object or to srcfile thus avoiding multiple instances created. Keep in mind
 * your threading model - by default this logger class checks for consistency of the call stack flow
 * and raises assertions if violated. This enforces a clean layout (i.e. threads separated explicitly).
 * If you think this is annoying, use option 2 or omit using enter/leave and stick to simple log
 * variants (v,d,i,w,e).
 *
 * __Usage Option 2 (uses functions only):__
 *
 * Put this to each class:
 * >      val CTAG = de.snfiware.szbsb.util.ctag("Main")
 * Put this to each method if needed:
 * >      val MTAG = de.snfiware.szbsb.util.mtag(CTAG,"method")
 * Log as usual:
 * >        Log.e(CTAG,"bar")
 * >      Log.i(MTAG,"foo")
 */
class AcmtLogger
{
    companion object {
        // all the stacks
        val mainStack = tStringStack()
        var furtherStacks = mutableMapOf<String,tStringStack>()
        //
        val LTAG = "${APPTAG_PLAIN}${APP_CLS_DELIM}ACMT"
        //
        fun push(stack:tStringStack,s:String) {
            // synchronized(block = {stack.push(s)},lock = "")?
            // No need to sync if bSeparateStack is used as intended and isolates concurrent threads
            if( stack.size >= MAXSTACKDEPTH ) {
                printStacks(Log.WARN)
                assert(false, "MAXSTACKDEPTH '$MAXSTACKDEPTH' reached")
            }
            stack.push(s)
            //
            if( stack.size == MAXSTACKDEPTH ) {
                Log.w(LTAG,"max stack depth reached - next push will FAIL")
            }
        }
        //
        fun pop(stack:tStringStack,sMethodTag:String,sClassTag:String) {
            val top = stack.peek()
            if(sMethodTag==EMPTY || !top.equals(sMethodTag) || !top.startsWith(sClassTag) || !sMethodTag.startsWith(sClassTag)) {
                printStacks(Log.WARN)
                assert(
                    false,
                    "peek is:'$top', got meth: '$sMethodTag' from '$sClassTag')"+
                            " - forgot to call enter/leave(ex)? called it too often?"
                )
            }
            stack.pop()
        }
        //
        private fun printStack(stackName :String, stack :tStringStack, out :StringBuilder) {
            out.append("${stackName}=${stack.size}:${stack}")
        }
        //
        fun printStacks(priority :Int =Log.VERBOSE) {
            val DELIM = ", "
            val out = StringBuilder(250)
            printStack("mainStack",mainStack,out)
            for(key in furtherStacks.keys) {
                out.append(DELIM)
                printStack(key, furtherStacks[key]!!, out)
            }
            if( priority >= LogHelper.getLoglevel() ) {
                Log.println(priority, LTAG, out.toString())
            }
        }
    }
    // end-companion
    ///////////////////////////////////////////////////
    // class attributes
    val sClassTagPlain :String
    val sClassTag :String
    var sMethodTag :String = EMPTY
    val stack :tStringStack
    // ctor
    constructor (sClassTagPlain :String, bSeparateStack :Boolean=false) {
        if( Log.DEBUG >= LogHelper.getLoglevel() ) {
            Log.d(LTAG, "new '$sClassTagPlain', bSeparateStack: $bSeparateStack")
        }
        //
        this.sClassTagPlain = sClassTagPlain
        this.sClassTag = ctag(sClassTagPlain)
        //
        if(bSeparateStack) {
            if(furtherStacks.keys.contains(sClassTagPlain)) {
                printStacks(Log.WARN)
                assert(false,"class tag $sClassTagPlain already exists as furtherStack")
            }
            else {
                furtherStacks[sClassTagPlain] = tStringStack()
            }
            stack = furtherStacks[sClassTagPlain]!!
        } else {
            stack = mainStack
        }
        printStacks()
    }
    // class methods
    fun log (sLogText :String, priority :Int =Log.DEBUG) {logIntern(sLogText,false,priority,APPEND_SRC,1)}
    fun log_(sLogText :String, priority :Int =Log.DEBUG) {logIntern(sLogText,true ,priority,APPEND_SRC,1)}
    //
    private fun logIntern (sLogText :String, bLogClassDespiteMethodPresent :Boolean =false, priority :Int =Log.DEBUG, bAppendSrc :Boolean =APPEND_SRC, nExtra :Int =0) {
        // get the first part of the log entry
        val tag = when (sMethodTag==EMPTY || bLogClassDespiteMethodPresent) {
            true -> sClassTag
            else -> sMethodTag // stack.peek() // these can differ if two classes share one stack and call each other
        }
        //
        if( priority >= LogHelper.getLoglevel() ) {
            // constant value for compiler: may optimize this better than dynamic len(sLogText)
            val sBuf = StringBuilder(MAXSTACKDEPTH + 50)
            //
            // if the tag is to short and alignment is requested, fill up with spaces
            for(i in 1..ALIGN_TO-tag.length){
                sBuf.append(" ")
            }
            // indicate stack depth by dots
            for(i in 2..stack.size){
                sBuf.append(".")
            }
            sBuf.append(sLogText)
            //
            if( bAppendSrc ) {
                val trc = Thread.currentThread().getStackTrace()
                var stack = trc[4+nExtra]
                // cannot distinguish safely *default* methods (parameter substitution - see stacktrace contents)
                // from others - debug and release build differs (some method calls are optimized and therefore gone)
                // + other (unknown) influences
//                if( stack.methodName.contains("default") ) {
//                    stack = trc[4+nExtra+1]
//                }
                sBuf.append(" (").append(stack.getFileName()).append(':').append(stack.getLineNumber()).append(')');
            }
            // finally we are ready to write one entry
            val sLogEntry = sBuf.toString()
            Log.println(priority, tag, sLogEntry)
            //
            if( CREATE_STATISTICS ) {
                Statistics.insertEntry(tag, priority, sLogEntry)
            }
        }
        //
        // check if caller got sth mixed up
        // do this after logging makes it more transparent where error occured
        if(!tag.startsWith(sClassTag))
            assert(false,"tag '$tag' is not from this instance '$sClassTag' - check enter/leave")
    }
    //
    fun enter (sMethodTagPlain: String, sLogText :String ="", priority :Int =Log.DEBUG, nExtra: Int =1) {
        sMethodTag = mtag(sClassTag,sMethodTagPlain)
        push(stack,sMethodTag) // have to be before log, thus log() uses stack.peek()
        logIntern("-> "+sLogText, false, priority, APPEND_SRC, nExtra)
    }
    fun enti (sMethodTagPlain: String) { enter(sMethodTagPlain, "", Log.INFO, 1) }
    fun enti (sMethodTagPlain: String, sLogText :String) { enter(sMethodTagPlain, sLogText, Log.INFO, 1) }
    //
    private fun getPrev() :String {
        var sRc = "class"
        if(stack.size >= 2) sRc = stack.elementAt(stack.size-2)
        return(sRc)
    }
    //
    private fun rundownStackUntilNextClassHit(stack :tStringStack, sClassTag :String) :String {
        var sRc = EMPTY
        var idx = stack.size
        while(idx-- > 0) {
            val s = stack[idx]
            if (s.startsWith(sClassTag)) {
                sRc = s
                break
            }
        }
        return(sRc)
    }
    //
    fun leave (sLogText :String ="", priority :Int =Log.DEBUG, nExtra: Int =1) {
        logIntern("<-"+"back2:[${getPrev()}] "+sLogText, false, priority, APPEND_SRC, nExtra)
        pop(stack,sMethodTag,sClassTag) // have to be after log, thus log() uses stack.peek()
        sMethodTag = when (stack.empty()) {
            true -> EMPTY
            else -> rundownStackUntilNextClassHit(stack,sClassTag)
        }
    }
    fun leavi () {leave("", Log.INFO, 1)}
    fun leavi (sLogText :String) {leave(sLogText, Log.INFO, 1)}
    //
    fun leave_ex (sLogText :String) {
        leave("EX! "+sLogText, Log.WARN, 2)
    }
    //
    fun v (sLogText :String) {logIntern(sLogText,false,Log.VERBOSE,APPEND_SRC,0)}
    fun d (sLogText :String) {logIntern(sLogText,false,Log.DEBUG  ,APPEND_SRC,0)}
    fun i (sLogText :String) {logIntern(sLogText,false,Log.INFO   ,APPEND_SRC,0)}
    fun w (sLogText :String) {logIntern(sLogText,false,Log.WARN   ,APPEND_SRC,0)}
    fun e (sLogText :String) {logIntern(sLogText,false,Log.ERROR  ,APPEND_SRC,0)}
    fun a (sLogText :String) {logIntern(sLogText,false,Log.ASSERT ,APPEND_SRC,0)}
    //
    fun v_ (sLogText :String) {logIntern(sLogText,true,Log.VERBOSE,APPEND_SRC,0)}
    fun d_ (sLogText :String) {logIntern(sLogText,true,Log.DEBUG  ,APPEND_SRC,0)}
    fun i_ (sLogText :String) {logIntern(sLogText,true,Log.INFO   ,APPEND_SRC,0)}
    fun w_ (sLogText :String) {logIntern(sLogText,true,Log.WARN   ,APPEND_SRC,0)}
    fun e_ (sLogText :String) {logIntern(sLogText,true,Log.ERROR  ,APPEND_SRC,0)}
    fun a_ (sLogText :String) {logIntern(sLogText,true,Log.ASSERT ,APPEND_SRC,0)}
}

//////////////////////////////////////////////////////////////////////////////////////////////

fun ctag( sClassTagPlain :String ) :String {
    assert(sClassTagPlain.length > 0, "input empty")
    assert(sClassTagPlain.length <= MAXLEN_CTAG, "input longer than ${MAXLEN_CTAG}")
    val sRc = APPTAG_PLAIN+APP_CLS_DELIM+sClassTagPlain
    assert(sRc.length <= MAXLEN_CTAG, "output longer than ${MAXLEN_CTAG}")
    return(sRc)
}

fun mtag( sClassTag :String, sMethodTagPlain :String ) :String {
    assert( sClassTag.startsWith(APPTAG_PLAIN), "app-prefix mandadory")
    assert(sMethodTagPlain.length > 0, "input empty")
    assert(sMethodTagPlain.length <= MAXLEN_MTAG, "input longer than ${MAXLEN_MTAG}")
    val sRc = sClassTag+CLS_MTH_DELIM+sMethodTagPlain
    assert(sRc.length <= MAXLEN_MTAG, "output longer than ${MAXLEN_MTAG}")
    return(sRc)
}

//////////////////////////////////////////////////////////////////////////////////////////////

/**
 * LogHelper
 *
 * This class implements a popup menu that can
 * - set the loglevel
 * - whitelist the current process to bypass chatty
 * - write statistics of used logtags to logcat
 * - export to file and share these excerpts from the logcat
 *
 * Integration as LongPress on the BackButton - put this code to your activity:
 * >     override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
 * >            val loghelper = LogHelper(this, this.fullActFooterHook, this.fullscreen_content)
 * >            loghelper.showPopupMenu(this)
 * >            return super.onKeyLongPress(keyCode, event)
 * >        }
 *
 */
open class LogHelper {
    companion object {
        val CTAG = AcmtLogger("LHlp")
        //
        val VERBOSE = Log.VERBOSE
        val DEBUG = Log.DEBUG
        val INFO = Log.INFO
        val WARN = Log.WARN
        val ERROR = Log.ERROR
        val ASSERT = Log.ASSERT
        val SILENT = 9
        //
        // The default loglevel - all log calls with priority below this level are skipped - above and including are logged
        private var myLoglevel = LOG_LEVEL
        private lateinit var myAct : AppCompatActivity
        //
        fun getLoglevel() : Int {return(myLoglevel)}
        fun setLoglevel( loglevel :Int ) : Int {
            val rc = myLoglevel
            myLoglevel = loglevel
            return( rc )
        }
        //
        fun getTimestamp(date :Date =Date(), pattern :String ="yyyy-MM-dd_HH:mm:ss", locale :Locale =Locale.GERMANY ) : String {
            val format = SimpleDateFormat(pattern, locale)
            val formattedDate = format.format(date.time)
            return formattedDate
        }
        //
        fun putCurrentPidToLogCatWhitelist() :Int {
            val pid = Process.myPid()
            val whiteList = "logcat -P '$pid'"
            Runtime.getRuntime().exec(whiteList).waitFor()
            return(pid)
        }
        //
        fun getLoglevelText( loglevel :Int ) : String {
            val sRc = when(loglevel) {
                VERBOSE -> "VERBOSE"
                DEBUG   -> "DEBUG"
                INFO    -> "INFO"
                WARN    -> "WARN"
                ERROR   -> "ERROR"
                ASSERT  -> "ASSERT"
                SILENT  -> "SILENT"
                else    -> "UNKNOWN"
            }
            return(sRc)
        }
        //
        fun getLoglevel1stChar( loglevel :Int ) : Char {
            val sRc = getLoglevelText(loglevel)[0]
            return(sRc)
        }
        //
        fun getLoglevelTextList() : List<String> {
            val sRc = listOf<String>  (
                  getLoglevelText(VERBOSE)
                , getLoglevelText(DEBUG  )
                , getLoglevelText(INFO   )
                , getLoglevelText(WARN   )
                , getLoglevelText(ERROR  )
                , getLoglevelText(ASSERT )
                , getLoglevelText(SILENT )
            )
            return(sRc)
        }
        //
        fun getLoglevelCharList() : List<Char> {
            val sRc = getLoglevelTextList().map { T -> T[0] }
            return(sRc)
        }
        //
        fun logStatisticsSummary() {
            val out = StringBuilder(10000)
            Statistics.printSummary(out)
            CTAG.i_(out.toString())
        }
        //
        fun logStats( priority: Int =Log.INFO) {
            CTAG.log( "vers: ${BuildConfig.VERSION_NAME}"
                    + " (${BuildConfig.FLAVOR}/${BuildConfig.BUILD_TYPE}/${BuildConfig.VERSION_CODE})"
                    + "; manu: ${Build.MANUFACTURER}; model: ${Build.MODEL}; device: ${Build.DEVICE}"
                    + "; sdk: ${Build.VERSION.SDK_INT}; release: ${Build.VERSION.RELEASE}.${Build.VERSION.INCREMENTAL}"
                    + "; build: ${Build.DISPLAY}; board: ${Build.BOARD}; brand: ${Build.BRAND}"
                    + "; cpu: ${Build.CPU_ABI}; bootload: ${Build.BOOTLOADER}; hw: ${Build.HARDWARE}"
                    + "; prod: ${Build.PRODUCT}; tags: ${Build.TAGS}; type: ${Build.TYPE}; unk: ${Build.UNKNOWN}"
                    + "; api23+: ${when(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                // AS-Inspection is fulfilled by when()
                                true -> "secPatch: ${Build.VERSION.SECURITY_PATCH}" +
                                        "; os: ${Build.VERSION.BASE_OS}"
                                else -> "0" }}"
                , priority // ; : ${Build.VERSION}; ${Build}
            )
        }
        //
        fun createLogAndShare(bAddDownload:Boolean, recipient:String) {
            CTAG.enter("creaLogAndShare", "bAddDownload: ${bAddDownload}, recipient: ${recipient}")
            logStats()
            //
            CTAG.d("getTimestamps...")
            val date = Date()
            val curTimestampFile = getTimestamp(date,"yyyy-MM-dd_HH.mm.ss")
            val curTimestampSubject = getTimestamp(date,"HH:mm:ss (dd.MM.yyyy)")

            CTAG.d("make cache/logs directory...")
            //val logPath = File(myAct.getExternalFilesDir(null), "logs") // /storage/emulated/0/Android/data/de.snfiware.szbsb/files/logs/
            val logPath = File(myAct.externalCacheDir, "logs")      // /storage/emulated/0/Android/data/de.snfiware.szbsb/cache/logs/
            //val logPath = File(myAct.cacheDir, "logs")                  // /data/data/de.snfiware.szbsb/cache/logs/
            logPath.mkdir()

            CTAG.d("prepare logfile...")
            val file = File(logPath,"${curTimestampFile}.log.txt")
            val filepath = file.absolutePath
            val cmd = "logcat -f $filepath"
            CTAG.i("execute shell cmd: $cmd")
            Runtime.getRuntime().exec(cmd) //.waitFor(5, TimeUnit.SECONDS)
            Thread.sleep(100) // there is need to wait a little - otherwise file might not be visible

            val authority = myAct.packageName + ".fileprovider"
            CTAG.d("authority: $authority; file.exists: ${file.exists()}; name: ${file.canonicalPath}")
            val uri = FileProvider.getUriForFile(myAct, authority, file)
            val mimeType: String = "text/plain"

            if( bAddDownload ) {
                CTAG.d("addCompletedDownload...")
                try {
                    val dm = myAct.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.addCompletedDownload(
                        "SzBsb/" + file.name,
                        //                    file.parentFile.parentFile.name + "/" + file.parentFile.name + "/" + file.name,
                        filepath,
                        true,
                        mimeType,
                        file.getAbsolutePath(),
                        file.length(),
                        true
                    )
                } catch (e: Exception) {
                    CTAG.e("EX! ${e.message}")
                    e.printStackTrace()
                }
            }

            CTAG.d("start MediaScanner.scanFile...")
            MediaScannerConnection.scanFile(myAct, arrayOf(file.getAbsolutePath()), arrayOf(mimeType), null)

            if( recipient != "" ) {
                CTAG.d("create new intent action.send...")
                val emailIntent = Intent(Intent.ACTION_SEND)
                emailIntent.setType(mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    emailIntent.putExtra( Intent.EXTRA_MIME_TYPES, mimeType )
                }
                //val packageName = "com.android.email"
                //emailIntent.setClassName(packageName, "com.android.mail.compose.ComposeActivity")
                emailIntent.setFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION )
                emailIntent.putExtra( Intent.EXTRA_STREAM, uri )
                emailIntent.putExtra( Intent.EXTRA_SUBJECT, "Log ${curTimestampSubject} SzBsb" )
                emailIntent.putExtra( Intent.EXTRA_EMAIL, arrayOf(recipient) )
                //
                val title = "Share/Send mail..."
                startActivity(myAct, Intent.createChooser(emailIntent, title),null)
                CTAG.leavi("started activity '$title'")
            }
        }
    }
    // end-of-companion
    //////////////////////////////////////////////////////////////////////////////////////////////
    // start-of-class
    //
    // The popup menu is shown on long-press of the back button - the place is determined by anchor
    val popupContext : Context
    val popupAnchor : View
    //
    // The view to place the snackbar.make.show output on
    val snackView : View
    val recipient : String
    //
    constructor(popupContext : Context, popupAnchor : View, snackView : View, recipient : String) {
        this.popupContext = popupContext
        this.popupAnchor = popupAnchor
        this.snackView = snackView
        this.recipient = recipient
    }
    //
    fun setLoglevel( loglevel :Int ) { //levelname :String ) {
        val old = LogHelper.setLoglevel(loglevel)
        showSnack( "Loglevel ${getLoglevelText(loglevel)} set (old: ${getLoglevelText(old)})" )
    }
    //
    fun showSnack(s:String, dur:Int = Snackbar.LENGTH_SHORT, bLog:Boolean =true ) {
        if(bLog) {CTAG.i("showing snack: $s")}
        Snackbar.make(snackView, s, dur).setAction("Action", null).show()
    }
    //
    fun showPopupMenu( act : AppCompatActivity ) {
        CTAG.enti("showPopupMenu","bring up popup menu upon: $act")
        myAct = act
        val popup = PopupMenu(popupContext,popupAnchor) //this.view_pager)
        popup.getMenuInflater().inflate(R.menu.log_menu, popup.getMenu())
        if( CREATE_STATISTICS == false ){
            popup.menu.findItem(R.id.mi_log_statistics).setVisible(false)
        }
        popup.setOnMenuItemClickListener(
            object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    CTAG.enti("PopUpClicked", "selected title: ${item.title}")
                    var bRc = true
                    val id = item.itemId
                    //
                    if( id == R.id.mi_share_log ) {
                        //
                        createLogAndShare(true, recipient)
                        showSnack("Logfile wurde in externen Cache exportiert und das 'Teilen'-Menü geöffnet.")
                    }
                    else if( id == R.id.mi_download_log ) {
                        //
                        createLogAndShare(true, "")
                        showSnack("Logfile im externen Cache. Benachrichtigung als Download.")
                    }
                    else if( id == R.id.mi_show_log_filter_by_pid ) {
                        // TODO
                        showSnack("TODO-NOOP: mi_show_log_filter_by_pid.")
                    }
                    else if(id == R.id.mi_log_statistics) {
                        //
                        logStatisticsSummary()
                        showSnack("Statistik wurde ins Log geschrieben.")
                    }
                    else if(id == R.id.mi_loglevel_verbose) {setLoglevel(Log.VERBOSE)}
                    else if(id == R.id.mi_loglevel_debug)   {setLoglevel(Log.DEBUG)}
                    else if(id == R.id.mi_loglevel_info)    {setLoglevel(Log.INFO)}
                    else if(id == R.id.mi_loglevel_warn)    {setLoglevel(Log.WARN)}
                    else if(id == R.id.mi_loglevel_error)   {setLoglevel(Log.ERROR)}
                    else if(id == R.id.mi_loglevel_assert)  {setLoglevel(Log.ASSERT)}
                    else if(id == R.id.mi_loglevel_silent)  {setLoglevel(LogHelper.SILENT)}
                    //  //
                    else if(id == R.id.mi_logcat_whitelist_pid) {
                        //
                        val pid = putCurrentPidToLogCatWhitelist()
                        showSnack("Schreibbeschränkung für PID $pid wurde ausgeschaltet.")
                    }
                    else if(id == R.id.mi_logcat_whitelist_aid) {
                        // TODO
                        val aId = ""
                        val uId = ""
                        showSnack("TODO-NOOP: Log-Limit für aId: $aId/uId: $uId dauerhaft entfernt.")
                    }
                    else if(id == R.id.mi_logcat_revoke_aid) {
                        // TODO
                        showSnack("TODO-NOOP: Log-Limit durch System ist wieder möglich.")
                    }
                    else {
                        bRc = false
                    }
                    //
                    CTAG.leave("bRc: $bRc")
                    return bRc
                }
            }
        )
        popup.show()
        //
        CTAG.leave()
    }
}
