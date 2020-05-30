/**
 * Copyright 2020 (Corona-Version) Schnuffiware - snuffo@freenet.de
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

import android.util.Log
import java.util.*

val APPTAG_PLAIN = "BZ" // adjust to your needs - bear in mind android logging tags have limit in length
val APP_CLS_DELIM = "." // this delimiter is put between the apptag and the classtag
val CLS_MTH_DELIM = "." // this delimiter is put between the classtag and the methodtag
val MAXSTACKDEPTH = 10  // if this limit is exceeded an assertion is risen
//
val ALIGN_TO = 23       // this should put the whole log flow starting from one row
val MAXLEN_CTAG = 23    // if this limit is exceeded an assertion is risen
val MAXLEN_MTAG = 23    // if this limit is exceeded an assertion is risen
val EMPTY = ""          // the empty value when no method is set (class scope logging only)

typealias tStringStack = Stack<String>

/**
 * Android Logger Class provides a clear und easy way to give unique tags to the log areas
 * you need and supports stack depth indention, therefore minimizing the need to
 * debug an app: the log (bug report / logcat) is sufficient for analysis. It has little overhead
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
 * Abstract: <APPTAG>.<CLASSTAG>.<METHODTAG>  padding stackdepth-indent   LOGTEXT
 *           | -------- max 23 chars -----|---opt.- | |-MAXSTACKDEPTH-|   your part...
 *                   ^          ^                   ^
 *       APP_CLS_DELIM          CLS_MTH_DELIM       ALIGN_TO
 *
 * Usage Option 1 - Example:
 * class Main {
 *      companion object {val CTAG = AcmtLogger("Main")}
 *      constructor {
 *          CTAG.enter("ctor","y")  // BZ.Main.ctor -> y
 *          CTAG.log_("xxx")        // BZ.Main xxx
 *          CTAG.log("wtf")         // BZ.Main.ctor wtf
 *          CTAG.enter("getX","17") // BZ.Main.getX .-> 17
 *          CTAG.log("wtf")         // BZ.Main.getX .wtf
 *          CTAG.leave("bRc:$bRc")  // BZ.Main.getX .<-back2:[BZ.Main.ctor] bRc:true
 *          CTAG.leave("z")         // BZ.Main.ctor <-back2:[class] z
 *          CTAG.log("xxx")         // BZ.Main xxx
 *      }
 * }
 *
 * If you invoke methods of another class which share the same method stack do it the same way
 * for this other class. It will share the indents. This will work perfectly for single-threaded
 * applications.
 *
 * Put the CTAG to companion object or to srcfile thus avoiding multiple instances created. Keep in mind
 * your threading model - by default this logger class checks for consistency of the call stack flow
 * and raises assertions if violated. This enforces a clean layout (i.e. threads separated to classes).
 * If you think this is annoying, use option 2 or omit using enter/leave and stick to simple log
 * variants (v,d,i,w,e).
 *
 * Usage Option 2 (uses functions only):
 * Put this to each class:
 *      val CTAG = de.snfiware.szbsb.util.ctag("Main")
 * Put this to each method:
 *      val MTAG = de.snfiware.szbsb.util.mtag(CTAG,"method")
 * Log as usual:
 *      Log.e(CTAG,"bar")
 *      Log.i(MTAG,"foo")
 */
class AcmtLogger
{
    companion object {
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
                Log.w(LTAG,"max stack depth reached - next push will fail")
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
        fun printStacks(priority :Int =Log.INFO) {
            val DELIM = ", "
            val out = StringBuilder(250)
            printStack("mainStack",mainStack,out)
            for(key in furtherStacks.keys) {
                out.append(DELIM)
                printStack(key, furtherStacks[key]!!, out)
            }
            Log.println(priority,LTAG,out.toString())
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
        Log.d(LTAG,"new '$sClassTagPlain', bSeparateStack: $bSeparateStack")
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
                //Log.d(LTAG,"created separated stack")
            }
            stack = furtherStacks[sClassTagPlain]!!
            //Log.v(LTAG,"created separated stack '$stack' for '$sClassTagPlain'")
        } else {
            stack = mainStack
            //Log.v(LTAG,"using main stack")
            //Log.v(LTAG,"using main stack '$stack' for '$sClassTagPlain'")
        }
        printStacks()
    }
    // class methods
    fun log (sLogText :String, priority :Int =Log.INFO) {logIntern(sLogText,priority = priority)}
    fun log_(sLogText :String, priority :Int =Log.INFO) {logIntern(sLogText,bLogClassDespiteMethodPresent = true ,priority = priority)}
    //
    private fun logIntern (sLogText :String, bLogClassDespiteMethodPresent :Boolean =false,priority :Int =Log.INFO) {
        // get the first part of the log entry
        val tag = when (sMethodTag==EMPTY || bLogClassDespiteMethodPresent) {
            true -> sClassTag
            else -> sMethodTag // stack.peek() // these can differ if two classes share one stack and call each other
        }
        // constant value for compiler: may optimize this better than dynamic len(sLogText)
        val sBuf = StringBuilder(MAXSTACKDEPTH + 50)
        // if the tag is to short and alignment is requested, fill up with spaces
        for(i in 1..ALIGN_TO-tag.length){
            sBuf.append(" ")
        }
        // indicate stack depth by dots
        for(i in 2..stack.size){
            sBuf.append(".")
        }
        sBuf.append(sLogText)
        Log.println(priority, tag, sBuf.toString())
        //
        // check if caller got sth mixed up
        // do this after logging makes it more transparent where error occured
        if(!tag.startsWith(sClassTag))
            assert(false,"tag '$tag' is not from this instance '$sClassTag' - check enter/leave")
    }
    //
    fun enter (sMethodTagPlain: String, sLogText :String ="") {
        //val sMethod = sMethodTagPlain //mtag(sClassTag,sMethodTagPlain)
        sMethodTag = mtag(sClassTag,sMethodTagPlain)
        push(stack,sMethodTag) // have to be before log, thus log() uses stack.peek()
        logIntern("-> "+sLogText)
    }
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
    fun leave (sLogText :String ="") {
        logIntern("<-"+"back2:[${getPrev()}] "+sLogText)
        pop(stack,sMethodTag,sClassTag) // have to be after log, thus log() uses stack.peek()
        sMethodTag = when (stack.empty()) {
            true -> EMPTY
            else -> rundownStackUntilNextClassHit(stack,sClassTag)
        }
    }
    //
    fun leave_ex (sLogText :String) {
        leave("EX! "+sLogText)
    }
    //
    fun v (sLogText :String) {logIntern(sLogText,priority = Log.VERBOSE)}
    fun d (sLogText :String) {logIntern(sLogText,priority = Log.DEBUG)}
    fun i (sLogText :String) {logIntern(sLogText,priority = Log.INFO)}
    fun w (sLogText :String) {logIntern(sLogText,priority = Log.WARN)}
    fun e (sLogText :String) {logIntern(sLogText,priority = Log.ERROR)}
    //fun a (sLogText :String) {logIntern(sLogText,priority = Log.ASSERT)}
    //
    fun v_ (sLogText :String) {logIntern(sLogText,bLogClassDespiteMethodPresent=true,priority = Log.VERBOSE)}
    fun d_ (sLogText :String) {logIntern(sLogText,bLogClassDespiteMethodPresent=true,priority = Log.DEBUG)}
    fun i_ (sLogText :String) {logIntern(sLogText,bLogClassDespiteMethodPresent=true,priority = Log.INFO)}
    fun w_ (sLogText :String) {logIntern(sLogText,bLogClassDespiteMethodPresent=true,priority = Log.WARN)}
    fun e_ (sLogText :String) {logIntern(sLogText,bLogClassDespiteMethodPresent=true,priority = Log.ERROR)}
    //fun a_ (sLogText :String) {logIntern(sLogText,bLogClassDespiteMethodPresent=true,priority = Log.ASSERT)}
}

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
