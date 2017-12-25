package sjj.fiction

import android.app.Fragment
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import sjj.permission.util.PermissionUtil
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.os.PersistableBundle
import org.jetbrains.anko.toast
import sjj.alog.Log
import sjj.permission.PermissionCallback
import sjj.permission.model.Permission


/**
 * Created by SJJ on 2017/10/5.
 */
abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.app.activitys.add(this)
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val requestedPermissions = packageInfo.requestedPermissions
        PermissionUtil.requestPermissions(this, requestedPermissions, object : PermissionCallback {

            override fun onGranted(permissions: Permission?) {
            }

            override fun onDenied(permissions: Permission?) {
                val s = "权限申请被拒绝：${permissions.toString()}"
                toast(s)
                Log.i(s)
            }
        })
        Log.i("onCreate $this")
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        Log.i("onCreate 2 $this")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.i("onAttachedToWindow $this")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.i("onTrimMemory $this")
    }

    override fun onAttachFragment(fragment: Fragment?) {
        super.onAttachFragment(fragment)
        Log.i("onAttachFragment $this  $fragment")
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        Log.i("onSaveInstanceState 2 $this")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("onRestart $this")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.i("onRestoreInstanceState $this")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        Log.i("onRestoreInstanceState 2 $this")
    }

    override fun onStart() {
        super.onStart()
        Log.i("onStart $this")
    }

    override fun onResume() {
        super.onResume()
        Log.i("onResume $this")
    }

    override fun onPause() {
        super.onPause()
        Log.i("onPause $this")
    }
    override fun onStop() {
        super.onStop()
        Log.i("onStop $this")
    }
    override fun onDestroy() {
        super.onDestroy()
        App.app.activitys.remove(this)
        Log.i("onDestroy $this")
    }
}