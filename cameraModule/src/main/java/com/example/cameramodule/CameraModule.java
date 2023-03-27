package com.example.cameramodule;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.cameramodule.java.LivePreviewActivity;
import com.taobao.weex.annotation.JSMethod;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.common.UniModule;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: ZhangJian
 * @date: 2023/2/23 16:04
 * @description:
 */
public class CameraModule extends UniModule {
    private static final String TAG = "CameraModule";

    public static int REQUEST_CODE = 1000;

    @UniJSMethod(uiThread = false)
    public JSONObject createView(String model) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 200);
        jsonObject.put("msg", "成功！");
        jsonObject.put("data", "传的参数为：" + model);
        return jsonObject;
    }

    @UniJSMethod(uiThread = true)
    public void createCamera(JSONObject options){
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            Intent intent = new Intent(mUniSDKInstance.getContext(), LivePreviewActivity.class);
            intent.putExtra("actionName", options.getString("actionName"));
            intent.putExtra("num", options.getString("num"));
            intent.putExtra("videoUrl", options.getString("videoUrl"));
            ((Activity)mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE && data.hasExtra("respond")) {
            String respond = data.getStringExtra("respond");
            Map<String,Object> params=new HashMap<>();
            params.put("respond",respond);
            mUniSDKInstance.fireGlobalEventCallback("cameraEvent", params);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
