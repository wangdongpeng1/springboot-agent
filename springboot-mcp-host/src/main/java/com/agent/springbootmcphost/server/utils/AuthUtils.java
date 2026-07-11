package com.agent.springbootmcphost.server.utils;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * api-auth 鉴权工具类
 *
 * <pre>
 * 参考：
 * https://aidocs.xfyun.cn/docs/public/http.html
 * https://aidocs.xfyun.cn/docs/public/api%E7%AD%BE%E5%90%8D.html
 * https://aidocs.xfyun.cn/docs/public/websocket.html
 * </pre>
 *
 * @author lyhu
 * @version 2019-10-08 16:01
 */
@Slf4j
public class AuthUtils {

    public static final String SKYNET_AUTH_ERR_MESSAGE = "SKYNET_AUTH_ERR_MESSAGE";
    public static final Charset CHARSET_UTF8 = StandardCharsets.UTF_8;
    public static final String ALGORITHM_NAME = "hmacsha256";
    public static final String DIGEST_NAME = "SHA-256";
    public static final String AUTH_KEY = "Authorization";
    public static final String DATE_FORMAT_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

    /**
     * 生成 API网关鉴权的 HTTP GET 请求参数URL
     * <p>
     * 例如：
     * wss://ws-api.xfyun.cn/v2/ivw?authorization=aG1hYyB1c2VybmFtZT0iMTAwSU1FIiwgYWxnb3JpdGhtPSJobWFjLXNoYTI1NiIsIGhlYWRlcnM9Imhvc3QgZGF0ZSByZXF1ZXN0LWxpbmUiLCBzaWduYXR1cmU9IlVSbnk4M3o1elJsNWF1ODl1YXhUL1dGdUtWejZVNkdkWDdDV25SMGdueWc9Ig%3D%3D&date=Tue%2C+18+Dec+2019+09%3A08%3A49+UTC&host=10.1.87.70%3A8000
     *
     * @param requestUrl
     * @param apiKey
     * @param apiSecret
     * @return
     */
    public static String assembleRequestUrl(String requestUrl, String apiKey, String apiSecret) {
        return assembleRequestUrl(HttpMethodEnum.GET.value(), requestUrl, apiKey, apiSecret);
    }

    /**
     * 生成 API网关鉴权的 HTTP POST 请求参数URL
     * <p>
     * 例如：
     * wss://ws-api.xfyun.cn/v2/ivw?authorization=aG1hYyB1c2VybmFtZT0iMTAwSU1FIiwgYWxnb3JpdGhtPSJobWFjLXNoYTI1NiIsIGhlYWRlcnM9Imhvc3QgZGF0ZSByZXF1ZXN0LWxpbmUiLCBzaWduYXR1cmU9IlVSbnk4M3o1elJsNWF1ODl1YXhUL1dGdUtWejZVNkdkWDdDV25SMGdueWc9Ig%3D%3D&date=Tue%2C+18+Dec+2019+09%3A08%3A49+UTC&host=10.1.87.70%3A8000
     *
     * @param requestUrl
     * @param apiKey
     * @param apiSecret
     * @return
     */
    public static String assemblePostRequestUrl(String requestUrl, String apiKey, String apiSecret) {
        return assembleRequestUrl(HttpMethodEnum.POST.value(), requestUrl, apiKey, apiSecret);
    }

    public static String assemblePutRequestUrl(String requestUrl, String apiKey, String apiSecret) {
        return assembleRequestUrl(HttpMethodEnum.PUT.value(), requestUrl, apiKey, apiSecret);
    }

    public static String assembleDeleteRequestUrl(String requestUrl, String apiKey, String apiSecret) {
        return assembleRequestUrl(HttpMethodEnum.DELETE.value(), requestUrl, apiKey, apiSecret);
    }

    /**
     * 生成 API网关鉴权的 HTTP 请求参数URL
     *
     * AppID: FCEDD717DED14C2BAB00;
     * App Secret Key: 6D923CC3E29D4585A606E662E89C7C1B
     * <p>
     * 例如：
     * wss://ws-api.xfyun.cn/v2/ivw?authorization=aG1hYyB1c2VybmFtZT0iMTAwSU1FIiwgYWxnb3JpdGhtPSJobWFjLXNoYTI1NiIsIGhlYWRlcnM9Imhvc3QgZGF0ZSByZXF1ZXN0LWxpbmUiLCBzaWduYXR1cmU9IlVSbnk4M3o1elJsNWF1ODl1YXhUL1dGdUtWejZVNkdkWDdDV25SMGdueWc9Ig%3D%3D&date=Tue%2C+18+Dec+2019+09%3A08%3A49+UTC&host=10.1.87.70%3A8000
     *
     * @param httpMethod 请求方法
     * @param requestUrl
     * @param apiKey
     * @param apiSecret
     * @return
     */
    public static String assembleRequestUrl(String httpMethod, String requestUrl, String apiKey, String apiSecret) {
        return assembleRequestUrl(httpMethod, requestUrl, apiKey, apiSecret, null);
    }

    public static String assembleRequestUrl(String httpMethod, String requestUrl, String apiKey, String apiSecret, String apiKeyKey) {
        AuthorizationData authorizationData = assemble(httpMethod, requestUrl, apiKey, apiSecret, null, apiKeyKey);

        try {
            String authBase = Base64.getEncoder().encodeToString(authorizationData.getAuthorization().getBytes(CHARSET_UTF8));
            authBase = String.format("%s%s%s=%s&host=%s&date=%s", requestUrl, !StrUtil.isBlank(new URI(requestUrl).getQuery()) ? "&" : "?", AUTH_KEY.toLowerCase(), URLEncoder.encode(authBase, "utf-8"),
                    URLEncoder.encode(authorizationData.getHost(), "utf-8"), URLEncoder.encode(authorizationData.getDate(), "utf-8"));
            log.debug("assembleRequestUrl:{}", authBase);
            return authBase;
        } catch (Throwable e) {
            log.error("assemble RequestUrl error ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成 API网关鉴权的 HTTP GET请求 Header 字典
     * <p>
     * Host: http://serverIP:port<p>
     * Date: Tue, 26 Jun 2020 12:27:03 UTC<p>
     * Digest: SHA-256=zc5lbDoNIhuwTUEzk+bu5OqCdHNxQxi4uKM5ghI2IDQ=<p>
     * Authorization: hmac api_key="alice123", algorithm="hmac-sha256", headers="host date request-line digest", signature="gaweQbATuaGmLrUr3HE0DzU1keWGCt3H96M28sSHTG8="
     *
     * @param requestUrl 完整请求URL
     * @param apiKey
     * @param apiSecret
     * @return
     */
    public static Map<String, String> assembleAuthorizationHeaders(String requestUrl, String apiKey, String apiSecret) {
        return assembleAuthorizationHeaders(HttpMethodEnum.GET.value(), requestUrl, apiKey, apiSecret, null, null);
    }

    /**
     * 生成 API网关鉴权的 HTTP请求 Header 字典
     * <p>
     * Host: http://serverIP:port<p>
     * Date: Tue, 26 Jun 2020 12:27:03 UTC<p>
     * Digest: SHA-256=zc5lbDoNIhuwTUEzk+bu5OqCdHNxQxi4uKM5ghI2IDQ=<p>
     * Authorization: hmac api_key="alice123", algorithm="hmac-sha256", headers="host date request-line digest", signature="gaweQbATuaGmLrUr3HE0DzU1keWGCt3H96M28sSHTG8="
     *
     * @param httpMethod 请求方法
     * @param requestUrl 完整请求URL
     * @param apiKey
     * @param apiSecret
     * @param body       请求的BODY
     * @return
     */
    public static Map<String, String> assembleAuthorizationHeaders(String httpMethod, String requestUrl, String apiKey, String apiSecret, String body) {
        return assembleAuthorizationHeaders(httpMethod, requestUrl, apiKey, apiSecret, body, null);
    }

    public static Map<String, String> assembleAuthorizationHeaders(String httpMethod, String requestUrl, String apiKey, String apiSecret, String body, String apiKeyKey) {
        AuthorizationData authorizationData = assemble(httpMethod, requestUrl, apiKey, apiSecret, body, apiKeyKey);
        return authorizationData.getHeader();
    }


    private static AuthorizationData assemble(String httpMethod, String requestUrl, String apiKey, String apiSecret, String body, String apiKeyKey) {

        if (ObjectUtil.isEmpty(requestUrl)) {
            throw new RuntimeException("requestUrl is empty.");
        }
        if (ObjectUtil.isEmpty(apiKey)) {
            throw new RuntimeException("apiKey is empty.");
        }
        if (ObjectUtil.isEmpty(apiSecret)) {
            throw new RuntimeException("apiSecret is empty.");
        }

        URL url;
        String httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://", "https://");
        try {
            url = new URL(httpRequestUrl);
            SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());

            String sha = getSignature(url.getHost(), date, getRequestLine(httpMethod, url.getPath()), apiSecret);

            String digest = null;
            if (!StrUtil.isBlank(body)) {
                digest = signBody(body);
            }
            apiKeyKey = !StrUtil.isBlank(apiKeyKey) ? apiKeyKey : "hmac api_key";
            String authorization = String.format("%s=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line%s\", signature=\"%s\"",
                    apiKeyKey, apiKey, !StrUtil.isBlank(digest) ? " digest" : "", sha);

            AuthorizationData authorizationData = new AuthorizationData();
            authorizationData.setDate(date).setHost(url.getHost()).setAuthorization(authorization).setDigest(digest);
            log.debug("authorizationData:\t[{}]", authorizationData);
            return authorizationData;
        } catch (Throwable e) {
            log.error("assemble AuthorizationData error", e);
            throw new RuntimeException(e);
        }
    }

    public static String getRequestLine(String method, String path) {
        return String.format("%s %s HTTP/1.1", method.toUpperCase(), path);
    }

    /**
     * @param host        172.31.97.147:8585 或  172.31.97.147:8585 或 skynet.iflytek.com
     * @param date        格式如： Fri, 20 Dec 2019 03:05:53 UTC
     * @param requestLine POST /nlp/v1/cws HTTP/1.1
     * @param apiSecret
     * @return
     */
    public static String getSignature(String host, String date, String requestLine, String apiSecret) {

        if (ObjectUtil.isEmpty(host)) {
            throw new RuntimeException("host is empty.");
        }
        if (ObjectUtil.isEmpty(date)) {
            throw new RuntimeException("date is empty.");
        }
        if (ObjectUtil.isEmpty(requestLine)) {
            throw new RuntimeException("requestLine is empty.");
        }
        if (ObjectUtil.isEmpty(apiSecret)) {
            throw new RuntimeException("apiSecret is empty.");
        }

        try {
            URI uri = new URI("skynet://" + host);
            StringBuilder builder = new StringBuilder("host: ").append(uri.getHost()).append("\n").
                    append("date: ").append(date).append("\n");
            builder.append(requestLine);

            log.debug("\n--signing string:---------------------------------------\n{}\n--signing string:---------------------------------------", builder);

            Mac mac = Mac.getInstance(ALGORITHM_NAME);

            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(CHARSET_UTF8), ALGORITHM_NAME);
            mac.init(spec);

            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(CHARSET_UTF8));
            String signature = Base64.getEncoder().encodeToString(hexDigits);

            log.debug("signature:{}", signature);
            return signature;
        } catch (Throwable e) {
            log.error("assemble AuthorizationData error", e);
            throw new RuntimeException(e);
        }
    }

    public static String signBody(String body) throws Exception {
        if (ObjectUtil.isEmpty(body)) {
            throw new RuntimeException("body is empty.");
        }
        try {
            return signBody(body.getBytes(StandardCharsets.UTF_8));
        } catch (UnsupportedEncodingException e) {
            log.error("Body签名失败：{}", e.getMessage());
        }
        return null;
    }

    public static String signBody(byte[] body) throws Exception {
        if (body == null || body.length == 0) {
            throw new IllegalArgumentException("body is empty.");
        }
        MessageDigest messageDigest;
        String digest = "";
        try {
            messageDigest = MessageDigest.getInstance(DIGEST_NAME);
            messageDigest.update(body);
            digest = Base64.getEncoder().encodeToString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("Body签名失败：{}", e.getMessage());
        }
        return digest;
    }


    public enum HttpMethodEnum {

        GET("GET"),
        POST("POST"),
        PUT("PUT"),
        DELETE("DELETE");
        private final String value;

        HttpMethodEnum(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    static class AuthorizationData {
        private String date;
        private String host;
        private String authorization;
        private String digest;

        public Map<String, String> getHeader() {
            Map<String, String> headers = new LinkedHashMap<>(4);

            headers.put("Host", host);
            headers.put("Date", date);

            if (!StrUtil.isBlank(digest)) {
                headers.put("Digest", String.format("%s=%s", DIGEST_NAME, digest));
            }
            headers.put(AUTH_KEY, authorization);
            return headers;
        }

        @Override
        public String toString() {
            return String.format("host=%s;date=%s;digest=%s;authorization=%s;", this.host, this.date, this.digest, this.authorization);
        }
    }
}
