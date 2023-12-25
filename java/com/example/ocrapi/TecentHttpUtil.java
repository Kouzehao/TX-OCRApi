package com.example.ocrapi;

import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TecentHttpUtil {

    // 定义一个简单的回调接口，用于处理网络请求的成功和失败
    public interface SimpleCallBack {
        void Succ(String result); // 成功时调用
        void error(); // 错误时调用
    }

    // 静态方法，用于获取身份证信息
    public static void getIdCardDetails(String bitmap, final SimpleCallBack callBack) {
        Map<String, Object> params = new HashMap<>(); // 创建一个参数映射
        params.put("ImageBase64", ImageToBase64(bitmap)); // 将图片转换为Base64字符串并放入参数映射

        Gson gson = new Gson(); // 创建Gson对象用于JSON处理
        String param = gson.toJson(params); // 将参数映射转换为JSON字符串

        // 调用SignUtil类的getAuthTC3方法发送请求，并接收返回的响应
        String response = SignUtil.getAuthTC3("IDCardOCR", param, "2018-11-19");

        if("".equals(response)){
            Log.d("onError", "信息识别错误"); // 如果返回响应为空，则记录错误信息
        }else{
            Log.d("Success", "信息识别正确"); // 否则记录成功信息
            callBack.Succ(response); // 调用回调接口的成功方法
        }
    }

    // 将图片转化为Base64编码的字符串
    public static String ImageToBase64(String img) {
        String imgFile = img; // 待处理的图片文件路径
        InputStream in = null;
        byte[] data = null;

        try {
            in = new FileInputStream(imgFile); // 打开文件输入流
            data = new byte[in.available()]; // 根据可读数据大小创建字节数组
            in.read(data); // 读取数据到数组中
            in.close(); // 关闭输入流
        } catch (IOException e) {
            e.printStackTrace(); // 异常处理
        }

        Base64Util encoder = new Base64Util(); // 创建Base64编码器
        return encoder.encode(data); // 返回Base64编码的字符串
    }
}