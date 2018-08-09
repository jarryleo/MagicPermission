package cn.leo.magicpermission

import android.Manifest
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import cn.leo.localnet.utils.toast
import cn.leo.permission.PermissionRequest

import kotlinx.android.synthetic.main.activity_main2.*

class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            testPermission()
        }
    }

    @PermissionRequest(Manifest.permission.CAMERA)
    private fun testPermission() {
        toast("申请权限成功")
    }
}
