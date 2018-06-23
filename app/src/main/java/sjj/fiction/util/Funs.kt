package sjj.fiction.util

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import sjj.alog.Log
import java.util.regex.Pattern

/**
 * Created by SJJ on 2017/10/15.
 */

fun String.domain(): String {
    val pattern = "(http(s)?://[a-zA-z\\d.]++)/?"
    val r = Pattern.compile(pattern)
    val m = r.matcher(this)
    if (m.find()) {
        return m.group(1)
    }
    return "error"
}

fun <T> T.log():T {
    if (this is Throwable) {
        Log.e(1, this,this)
    } else {
        Log.e(1,this)
    }
    return this
}

fun Throwable.stackTraceString(): String {
    val buffer = StringBuilder()
    buffer.append(this::class.java).append(", ").append(message).append("\n")
    var throwable: Throwable? = this
    while (throwable != null) {
        for (element in throwable.stackTrace) {
            buffer.append(element.toString()).append("\n")
        }
        throwable = throwable.cause
        if (throwable != null)
            buffer.append("caused by ")
    }
    return buffer.toString()
}

inline fun <reified T : Fragment> FragmentActivity.getFragment(containerViewId: Int = 0, tag: String): T {
    val byTag = supportFragmentManager.findFragmentByTag(tag) ?: T::class.java.newInstance().also {
        supportFragmentManager.beginTransaction().add(containerViewId, it, tag).commit()
    }
    return byTag as T

}