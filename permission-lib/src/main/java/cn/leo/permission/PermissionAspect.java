package cn.leo.permission;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Leo on 2018/5/2.
 */
@Aspect
public class PermissionAspect {
    private static final String POINTCUT_METHOD =
            "execution(@cn.leo.permission.PermissionRequest * *(..))";

    @Pointcut(POINTCUT_METHOD)
    public void methodAnnotatedWithPermission() {
    }

    @Around("methodAnnotatedWithPermission()")
    public void aroundJoinPoint(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PermissionRequest annotation = method.getAnnotation(PermissionRequest.class);
        String[] permission = annotation.value();
        final Object target = joinPoint.getTarget();
        FragmentActivity fragmentActivity;
        if (target instanceof FragmentActivity) {
            fragmentActivity = (FragmentActivity) target;
        } else if (target instanceof Fragment) {
            fragmentActivity = ((Fragment) target).getActivity();
        } else {
            throw new PermissionRequestException(
                    "The annotation permission can only be used in FragmentActivity or Fragment environment and its subclass environment.");
        }
        final FragmentActivity finalFragmentActivity = fragmentActivity;
        PermissionUtil.getInstance(fragmentActivity)
                .request(permission)
                .execute(new PermissionUtil.Result() {
                    @Override
                    public void onSuccess() {
                        try {
                            joinPoint.proceed();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailed(String[] failedPermissions) {
                        Method failedCallBack = findFailedCallback(target);
                        if (failedCallBack == null) {
                            showFailedToast();
                            return;
                        }
                        Class<?>[] types = failedCallBack.getParameterTypes();
                        try {
                            if (types.length == 1 &&
                                    types[0].isArray() &&
                                    types[0].getComponentType() == String.class) {
                                failedCallBack.invoke(target, (Object) failedPermissions);
                            } else if (types.length == 0) {
                                failedCallBack.invoke(target);
                            } else {
                                showFailedToast();
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }

                    public void showFailedToast() {
                        Toast.makeText(finalFragmentActivity, finalFragmentActivity.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    public Method findFailedCallback(Object object) {
        Class<?> aClass = object.getClass();
        for (Method method : aClass.getDeclaredMethods()) {
            boolean isCallback = method.isAnnotationPresent(PermissionRequestFailedCallback.class);
            if (!isCallback) continue;
            method.setAccessible(true);
            return method;
        }
        return null;
    }
}
