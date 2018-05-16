package cn.leo.permission;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;

/**
 * Created by JarryLeo on 2018/2/6.
 * 安卓8.0以下申请一个权限，用户同意后整个权限组的权限都不用申请可以直接使用
 * 8.0后每个权限都要单独申请，不能一次申请通过后,整个权限组都不用申请;
 * 但是用户同意权限组内一个之后，其它申请直接通过(之前是不申请可以直接用，现在是申请直接过(但是必须申请))
 */

public class PermissionUtil {
    private static String tag = "fragmentRequestPermissionCallBack";
    private static final int REQUEST_CODE = 110;
    private FragmentCallback mFragmentCallback;

    public interface Result {
        void onSuccess();

        void onFailed();
    }

    /**
     * fragment，作为权限回调监听，和从设置界面返回监听
     */
    public static class FragmentCallback extends Fragment {
        private Result mResult;
        private String[] mPermissions;
        private long mRequestTime;

        public void setRequestTime() {
            mRequestTime = SystemClock.elapsedRealtime();
        }

        public void setResult(Result result) {
            mResult = result;
        }

        public void setPermissions(String[] permissions) {
            mPermissions = permissions;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            boolean result = true;
            switch (requestCode) {
                case REQUEST_CODE:
                    for (int i = 0; i < permissions.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            result = false;
                            break;
                        }
                    }
                    break;
            }
            if (mResult != null) {
                if (result) {
                    detach();
                    mResult.onSuccess();
                } else {
                    if (SystemClock.elapsedRealtime() - mRequestTime < 300) {
                        StringBuilder sb = new StringBuilder();
                        for (String permission : mPermissions) {
                            if (!PermissionUtil.checkPermission(getActivity(), permission)) {
                                String permissionName = getPermissionName(getActivity(), permission);
                                if (!permissionName.isEmpty()) {
                                    sb.append(" [")
                                            .append(permissionName)
                                            .append("] ");
                                }
                            }
                        }
                        String permissionList = sb.toString();
                        String s = permissionList.replaceAll("(\\s\\[.*\\]\\s)\\1+", "$1");
                        openSettingActivity(getString(R.string.permission_should_show_rationale, s));
                    } else {
                        mResult.onFailed();
                    }
                }
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_CODE) {
                if (mResult != null && mPermissions != null) {
                    boolean result = true;
                    for (String mPermission : mPermissions) {
                        if (!checkPermission(getActivity(), mPermission)) {
                            result = false;
                            break;
                        }
                    }
                    if (result) {
                        detach();
                        mResult.onSuccess();
                    } else {
                        mResult.onFailed();
                    }
                }
            }
        }

        //解绑fragment
        private void detach() {
            if (!isAdded()) return;
            FragmentTransaction fragmentTransaction =
                    getFragmentManager().beginTransaction();
            fragmentTransaction.detach(this);
            fragmentTransaction.remove(this);
            fragmentTransaction.commitAllowingStateLoss();
        }

        /**
         * 打开应用权限设置界面
         */
        public void openSettingActivity(String message) {
            showMessageOKCancel(message, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQUEST_CODE);
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mResult.onFailed();
                }
            });
        }

        /**
         * 弹出对话框
         *
         * @param message    消息内容
         * @param okListener 点击回调
         */
        private void showMessageOKCancel(String message,
                                         DialogInterface.OnClickListener okListener,
                                         DialogInterface.OnClickListener cancelListener) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.permission_dialog_granted), okListener)
                    .setNegativeButton(getString(R.string.permission_dialog_denied), cancelListener)
                    .create()
                    .show();
        }
    }

    private FragmentActivity mActivity;
    private String[] mPermissions;

    private PermissionUtil(FragmentActivity activity) {
        this.mActivity = activity;

    }

    /**
     * 获取请求权限实例
     *
     * @param activity FragmentActivity
     * @return 请求权限工具对象
     */
    public static PermissionUtil getInstance(FragmentActivity activity) {
        return new PermissionUtil(activity);
    }

    /**
     * 需要请求的权限列表
     *
     * @param permissions 权限列表
     * @return 返回自身链式编程
     */
    public PermissionUtil request(String... permissions) {
        mPermissions = permissions;
        return this;
    }

    /**
     * 执行权限请求
     *
     * @param result 请求结果回调
     */
    public void execute(Result result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkPermissions()) {
            if (result != null) {
                result.onSuccess();
            }
            return;
        }
        //创建fragment回调
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        Fragment fragmentByTag = fragmentManager.findFragmentByTag(tag);
        if (fragmentByTag != null) {
            mFragmentCallback = (FragmentCallback) fragmentByTag;
            mFragmentCallback.setResult(result);
        } else {
            mFragmentCallback = new FragmentCallback();
            mFragmentCallback.setResult(result);
            fragmentManager
                    .beginTransaction()
                    .add(mFragmentCallback, tag)
                    .commit();
            fragmentManager.executePendingTransactions();
        }
        //开始请求
        requestPermission();
    }

    /**
     * 检查权限列表是否全部通过
     *
     * @return 权限列表是否全部通过
     */
    private boolean checkPermissions() {
        for (String mPermission : mPermissions) {
            if (!checkPermission(mPermission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查权限
     *
     * @param permission 权限列表
     * @return 权限是否通过
     */
    private boolean checkPermission(String permission) {
        //检查权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        int checkSelfPermission =
                ContextCompat
                        .checkSelfPermission(mActivity, permission);
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 静态检查权限
     *
     * @param context    上下文
     * @param permission 权限列表
     * @return 权限是否通过
     */
    private static boolean checkPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        int checkSelfPermission =
                ContextCompat
                        .checkSelfPermission(context, permission);
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 获取权限的名称,自动按设备语言显示
     *
     * @param context    上下文
     * @param permission 权限
     * @return 权限名称
     */
    private static String getPermissionName(Context context, String permission) {
        String permissionName = "";
        PackageManager pm = context.getPackageManager();
        try {
            PermissionInfo permissionInfo = pm.getPermissionInfo(permission, 0);
            PermissionGroupInfo groupInfo = pm.getPermissionGroupInfo(permissionInfo.group, 0);
            permissionName = groupInfo.loadLabel(pm).toString();

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return permissionName;
    }

    /**
     * 申请权限
     */
    private void requestPermission() {

        if (mActivity.getSupportFragmentManager().findFragmentByTag(tag) == null) {
            throw new PermissionRequestException(mActivity.getString(R.string.permission_request_exception));
        }
        if (mFragmentCallback != null && mPermissions != null) {
            mFragmentCallback.setPermissions(mPermissions);
            //提取权限列表里面没通过的
            String[] per = new String[mPermissions.length];
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mPermissions.length; i++) {
                per[i] = mPermissions[i];
                if (!checkPermission(mPermissions[i])) {
                    String permissionName = getPermissionName(mActivity, mPermissions[i]);
                    if (!permissionName.isEmpty()) {
                        sb.append(" [")
                                .append(permissionName)
                                .append("] ");
                    }
                }
            }
            String permissionList = sb.toString();
            String s = permissionList.replaceAll("(\\s\\[.*\\]\\s)\\1+", "$1");
            //如果用户点了不提示(或者同时申请多个权限)，我们主动提示用户
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, mPermissions[0])) {
                mFragmentCallback.openSettingActivity(
                        mActivity.getString(R.string.permission_should_show_rationale, s));
            } else {
                //申请权限
                try {
                    mFragmentCallback.setRequestTime();
                    mFragmentCallback.requestPermissions(per, REQUEST_CODE);
                } catch (Exception e) {
                    mFragmentCallback.openSettingActivity(
                            mActivity.getString(R.string.permission_should_show_rationale, s));
                }
            }
        }
    }
}

/*•	Normal Permissions如下 （不需要动态申请，只需要在清单文件注册即可）

ACCESS_LOCATION_EXTRA_COMMANDS
ACCESS_NETWORK_STATE
ACCESS_NOTIFICATION_POLICY
ACCESS_WIFI_STATE
BLUETOOTH
BLUETOOTH_ADMIN
BROADCAST_STICKY
CHANGE_NETWORK_STATE
CHANGE_WIFI_MULTICAST_STATE
CHANGE_WIFI_STATE
DISABLE_KEYGUARD
EXPAND_STATUS_BAR
GET_PACKAGE_SIZE
INSTALL_SHORTCUT
INTERNET
KILL_BACKGROUND_PROCESSES
MODIFY_AUDIO_SETTINGS
NFC
READ_SYNC_SETTINGS
READ_SYNC_STATS
RECEIVE_BOOT_COMPLETED
REORDER_TASKS
REQUEST_INSTALL_PACKAGES
SET_ALARM
SET_TIME_ZONE
SET_WALLPAPER
SET_WALLPAPER_HINTS
TRANSMIT_IR
UNINSTALL_SHORTCUT
USE_FINGERPRINT
VIBRATE
WAKE_LOCK
WRITE_SYNC_SETTINGS


•Dangerous Permissions: (需要动态申请，当然也要在清单文件声明)

group:android.String-group.CONTACTS
  String:android.String.WRITE_CONTACTS
  String:android.String.GET_ACCOUNTS
  String:android.String.READ_CONTACTS

group:android.String-group.PHONE
  String:android.String.READ_CALL_LOG
  String:android.String.READ_PHONE_STATE
  String:android.String.CALL_PHONE
  String:android.String.WRITE_CALL_LOG
  String:android.String.USE_SIP
  String:android.String.PROCESS_OUTGOING_CALLS
  String:com.android.voicemail.String.ADD_VOICEMAIL

group:android.String-group.CALENDAR
  String:android.String.READ_CALENDAR
  String:android.String.WRITE_CALENDAR

group:android.String-group.CAMERA
  String:android.String.CAMERA

group:android.String-group.SENSORS
  String:android.String.BODY_SENSORS

group:android.String-group.LOCATION
  String:android.String.ACCESS_FINE_LOCATION
  String:android.String.ACCESS_COARSE_LOCATION

group:android.String-group.STORAGE
  String:android.String.READ_EXTERNAL_STORAGE
  String:android.String.WRITE_EXTERNAL_STORAGE

group:android.String-group.MICROPHONE
  String:android.String.RECORD_AUDIO

group:android.String-group.SMS
  String:android.String.READ_SMS
  String:android.String.RECEIVE_WAP_PUSH
  String:android.String.RECEIVE_MMS
  String:android.String.RECEIVE_SMS
  String:android.String.SEND_SMS
  String:android.String.READ_CELL_BROADCASTS

*/
