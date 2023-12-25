package com.example.ocrapi;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.example.ocrapi.TecentHttpUtil;
import com.example.ocrapi.IdentifyResult;
import android.Manifest;
import com.google.gson.Gson;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private Button btn_scan; // 定义用于启动扫描的按钮
    private File outputImage; // 保存拍摄到的照片的文件
    private TextView textView; // 用于显示识别结果的TextView
    private IdentifyResult identifyResult; // 用于保存身份证识别结果的对象
    private static final int REQUEST_CAMERA_PERMISSION = 1; // 定义请求相机权限的请求码

    // 内部类，用于表示从OCR服务返回的响应数据
    public class ResponseData {
        private IdentifyResult Response; // 封装了身份证识别的响应数据
        public IdentifyResult getResponse() { // 获取识别结果
            return Response;}
        public void setResponse(IdentifyResult response) { // 设置识别结果
            Response = response;}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 设置应用程序布局
        // 初始化UI组件
        this.btn_scan = (Button) findViewById(R.id.button); // 获取扫描按钮
        this.textView = (TextView) findViewById(R.id.textView); // 获取用于显示识别结果的TextView

        // 为扫描按钮设置点击事件监听器
        this.btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 检查相机权限，如果没有则请求权限
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }

                // 创建用于存储拍摄照片的文件
                MainActivity.this.outputImage = new File(MainActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "output.jpg");

                // 创建Intent启动相机应用
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                Uri imageUri = FileProvider.getUriForFile(MainActivity.this,
                        MainActivity.this.getApplicationContext().getPackageName() +
                                ".provider", MainActivity.this.outputImage);
                intent.putExtra("output", imageUri); // 设置照片保存位置
                MainActivity.this.launcher.launch(intent); // 启动相机应用
            }
        });
    }

    // 定义处理身份证识别的方法
    private void IdCardDetails() {
        if (this.outputImage.exists()) { // 检查拍摄的照片文件是否存在
            String imagePath = this.outputImage.toString();
            // 调用腾讯云OCR接口进行身份证识别
            TecentHttpUtil.getIdCardDetails(imagePath, new TecentHttpUtil.SimpleCallBack() {
                @Override
                public void Succ(String result) { // 接收识别成功的结果
                    Log.d("kzh", "result: " + result); // 记录调用ocr的结果日志
                    Gson gson = new Gson();
                    ResponseData responseData = gson.fromJson(result, ResponseData.class); // 解析返回的JSON字符串
                    IdentifyResult identifyResult = responseData.getResponse(); // 获取识别结果

                    // 构建并显示格式化的识别结果
                    String formattedResult = "姓名: " + identifyResult.getName()
                            + "\n性别: " + identifyResult.getSex() +
                            "\n民族: " + identifyResult.getNation() +
                            "\n身份证号码: " + identifyResult.getId()  +
                            "\n出生日期: " + identifyResult.getBirth() +
                            "\n住址: " + identifyResult.getAddress();
                    MainActivity.this.textView.setText(formattedResult);
                }

                @Override
                public void error() { // 在识别失败时调用
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "身份证识别失败", Toast.LENGTH_LONG).show(); // 显示错误提示
                    });
                }
            });
            return;
        }
        Toast.makeText(this, "找不到图片", Toast.LENGTH_LONG).show(); // 如果没有找到照片，显示提示
    }

    // 定义处理相机应用返回结果的ActivityResultLauncher
    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>()
            {
        @Override
        public void onActivityResult(ActivityResult result) { // 处理相机应用返回的结果
            if (result.getResultCode() == Activity.RESULT_OK) { // 判断返回结果是否成功
                Log.d("kzh", "Image path: " + MainActivity.this.outputImage.getAbsolutePath()); // 记录照片路径
                MainActivity.this.IdCardDetails(); // 调用身份证识别方法
            }
        }
    });
}