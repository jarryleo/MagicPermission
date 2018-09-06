# MagicPermission
安卓纯注解动态权限申请库

在需要权限的方法上打上注解，全自动处理动态权限各种问题：

自动处理用户同意/拒绝操作；

自动处理用户拒绝并勾选不在提示后的 弹框提示，并跳转到设置界面引导用户开启权限；

用户在设置界面返回后自动处理  设置界面操作的结果；

兼容国产rom；

#### 使用示例：

单个权限申请
```
@PermissionRequest(Manifest.permission.CAMERA)
public void testPermission() {
        //执行权限申请通过后的业务逻辑
}
```

多个权限同时申请
```
@PermissionRequest({Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE})
public void testPermission() {
        //执行权限申请通过后的业务逻辑
}
```
权限申请失败回调（如果不需要自己处理失败结果，可以忽略）          
给任意方法打上 注解 @PermissionRequestFailedCallback 即可         
打上此注解的方法 即为权限申请失败回调，方法的参数必须为 String[] 或者没有参数；          
当参数为String[] 时候，结果为失败的权限数组；        

```
@PermissionRequestFailedCallback
private void failed(String[] failedPermissions) {
     Toast.makeText(this, "申请权限失败" + Arrays.toString(failedPermissions), Toast.LENGTH_SHORT).show();
}
```

### 注意:只能在Fragment(v4)和FragmentActivity 以及它们的子类 中使用
### 不要把注解打到有生命周期的方法上，否则可能会导致生命周期被拦截

### 依赖方法:
#### To get a Git project into your build:
#### Step 1. Add the JitPack repository to your build file
1.在全局build里面添加下面github仓库地址

Add it in your root build.gradle at the end of repositories:
```
buildscript {
    ...
    dependencies {
	...
        classpath 'cn.leo.plugin:magic-plugin:1.0.0' //java 用这个
	classpath 'com.hujiang.aspectjx:gradle-android-plugin-aspectjx:2.0.0' //kotlin 用这个
    }
}
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
google()和jcenter()这两个仓库一般是默认的，如果没有请加上            
*上面2个build里面的 java 和 kotlin 二选一,如果AS版本低于3.0 请使用kotlin 版本

#### Step 2. Add the dependency
2.在app的build里面添加插件和依赖
```
...
apply plugin: 'cn.leo.plugin.magic' //java 用这个
apply plugin: 'android-aspectjx'  //kotlin 用这个，编译速度会慢点
...
dependencies {
	...
	implementation 'com.github.jarryleo:MagicPermission:v1.4'
}
```

> 用于支持kotlin的插件用的是 [aspectjx](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx)   
> 感谢插件作者    
> 因为编织所有二进制文件的问题导致编译速度慢的问题，请查看原作者提供的解决方案 

### 小贴士：
在 app 的 build 依赖里再加一个依赖：

```
implementation 'com.github.jarryleo:MagicThread:v2.2'
```
## 即可使用安卓纯注解线程转换库
example:
```
    @RunOnIOThread
    public void progress() {
        for (int i = 0; i <= 100; i++) {
            showProgress(i);
            SystemClock.sleep(1000);
        }
    }

    @RunOnUIThread
    private void showProgress(int progress) {
        mTvTest.setText(progress + "%");
    }
```

详情见 [安卓纯注解线程转换库](https://github.com/jarryleo/MagicThread)
