package cn.leo.magicpermission;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;

import cn.leo.permission.PermissionRequest;
import cn.leo.permission.PermissionRequestFailedCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnRequest = findViewById(R.id.btnRequest);
        btnRequest.setOnClickListener(this);
    }

    @Override
    @PermissionRequest({Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE})
    public void onClick(View v) {
        Toast.makeText(this, "申请权限成功", Toast.LENGTH_SHORT).show();
    }

    @PermissionRequestFailedCallback
    private void failed(String[] failedPermissions) {
        Toast.makeText(this, "申请权限失败" + Arrays.toString(failedPermissions), Toast.LENGTH_SHORT).show();
    }

    private void failed() {
        Toast.makeText(this, "申请权限失败", Toast.LENGTH_SHORT).show();
    }
}
