package com.example.ocrapi;

import android.os.StrictMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SignUtil {
    private static String SecretId = "";//传入自己的secretId
    private static String SecretKey = "";//传入自己的secretKey
    private static String Url = "https://ocr.tencentcloudapi.com";
    // ************* 步骤 1：拼接规范请求串 *************
    private static String HTTPRequestMethod = "POST";
    private static String CanonicalURI = "/";
    private static String CanonicalQueryString = "";
    private static String CanonicalHeaders = "content-type:application/json; charset=utf-8\nhost:ocr.tencentcloudapi.com\n";
    private static String SignedHeaders = "content-type;host";//参与签名的头部信息

    //签名字符串
    private static String Algorithm = "TC3-HMAC-SHA256";
    private static String Service = "ocr";
    private static String Stop = "tc3_request";

    //版本
    public static String Version = "2018-11-19";
    public static String Region = "ap-guangzhou";

    //v3鉴权
    public static String getAuthTC3(String action, String paramJson, String version) {
        try {
            String hashedRequestPayload = HashEncryption(paramJson);
            String CanonicalRequest =
                    HTTPRequestMethod + '\n' +  // HTTP 请求方法
                            CanonicalURI + '\n' +   // 请求的 URI
                            CanonicalQueryString + '\n' +   // 查询字符串
                            CanonicalHeaders + '\n' +   // 规范的头部信息字符串
                            SignedHeaders + '\n' +  // 签名头部信息
                            hashedRequestPayload;   // 基于请求负载（如 JSON 请求体）的 SHA-256 哈希值。
            //时间戳
            Date date = new Date();
            //微秒->秒
            String timestamp = String.valueOf(date.getTime() / 1000);

            //格林威治时间转化
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            String dateString = formatter.format(date.getTime());

            // ************* 步骤 2：拼接待签名字符串 *************
            String credentialScope = dateString + "/" + Service + "/" + Stop;
            String hashedCanonicalRequest = HashEncryption(CanonicalRequest);
            String stringToSign = Algorithm + "\n" +
                    timestamp + "\n" +
                    credentialScope + "\n" +
                    hashedCanonicalRequest;

            // ************* 步骤 3：计算签名 *************
            // 生成一个秘密日期键
            byte[] secretDate = HashHmacSha256Encryption(("TC3" + SecretKey).getBytes("UTF-8"), dateString);
            // 秘密日期键加密服务名称（如 "ocr"）生成秘密服务键。
            byte[] secretService = HashHmacSha256Encryption(secretDate, Service);
            // 使用秘密服务键加密 "tc3_request" 生成签名键。
            byte[] secretSigning = HashHmacSha256Encryption(secretService, Stop);
            // 使用签名键对第二步中的待签名字符串进行 HMAC-SHA256 加密，生成签名。
            byte[] signatureHmacSHA256 = HashHmacSha256Encryption(secretSigning, stringToSign);

            StringBuilder builder = new StringBuilder();
            for (byte b : signatureHmacSHA256) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                builder.append(hex);
            }
            String signature = builder.toString().toLowerCase();
            // ************* 步骤 4：拼接 Authorization *************
            String authorization = Algorithm + ' ' +    // 加密算法标识
                    // Credential 凭证。包含 SecretId + 第二步中的凭证范围
                    "Credential=" + SecretId + '/' + credentialScope + ", " +
                    // SignedHeaders签名头部信息
                    "SignedHeaders=" + SignedHeaders + ", " +
                    // 上一步生成的签名
                    "Signature=" + signature;

            //创建header 头部
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", authorization);
            headers.put("Host", "ocr.tencentcloudapi.com");
            headers.put("Content-Type", "application/json; charset=utf-8");
            headers.put("X-TC-Action", action);
            headers.put("X-TC-Version", version);
            headers.put("X-TC-Timestamp", timestamp);
            headers.put("X-TC-Region", Region);
            //request 请求 获取json数据
            String response = resquestPostData(Url, paramJson, headers);
            return response;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /*
     * Function  :   发送Post请求到服务器
     * Param     :   params请求体内容，encode编码格式
     */
    public static String resquestPostData(String strUrlPath, String data, Map<String, String> headers) {
        try {
            URL url = new URL(strUrlPath);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(3000);            //设置连接超时时间
            httpURLConnection.setDoInput(true);                  //打开输入流，以便从服务器获取数据
            httpURLConnection.setDoOutput(true);                 //打开输出流，以便向服务器提交数据
            httpURLConnection.setRequestMethod("POST");          //设置以Post方式提交数据
            httpURLConnection.setUseCaches(false);               //使用Post方式不能使用缓存
            //设置header
            if (headers.isEmpty()) {
                //设置请求体的类型是文本类型
                httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            } else {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    httpURLConnection.setRequestProperty(key, value);
                }
            }
            //设置请求体的长度
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(data.length()));
            //获得输出流，向服务器写入数据
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build());
            if (data != null) {
                byte[] writebytes = data.getBytes();
                // 设置文件长度
                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(data.getBytes());
                outputStream.flush();
            }
            int response = httpURLConnection.getResponseCode();            //获得服务器的响应码
            if (response == HttpURLConnection.HTTP_OK) {
                InputStream inptStream = httpURLConnection.getInputStream();
                return dealResponseResult(inptStream);                     //处理服务器的响应结果
            }
        } catch (IOException e) {
            return "err: " + e.getMessage().toString();
        }
        return "-1";
    }

    /*
     * Function  :   封装请求体信息
     * Param     :   params请求体内容，encode编码格式
     */
    public static StringBuffer getRequestData(Map<String, String> params, String encode) {
        StringBuffer stringBuffer = new StringBuffer();        //存储封装好的请求体信息
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                stringBuffer.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), encode))
                        .append("&");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);    //删除最后的一个"&"
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }

    /*
     * Function  :   处理服务器的响应结果（将输入流转化成字符串）
     * Param     :   inputStream服务器的响应输入流
     */
    public static String dealResponseResult(InputStream inputStream) {
        String resultData = null;      //存储处理结果
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        try {
            while ((len = inputStream.read(data)) != -1) {
                byteArrayOutputStream.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        resultData = new String(byteArrayOutputStream.toByteArray());
        return resultData;
    }

    private static String HashEncryption(String s) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(s.getBytes());
        //替换java DatatypeConverter.printHexBinary(d).toLowerCase()
        StringBuilder builder = new StringBuilder();
        for (byte b : sha.digest()) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            builder.append(hex);
        }
        return builder.toString().toLowerCase();
    }

    private static byte[] HashHmacSha256Encryption(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes("UTF-8"));
    }


}