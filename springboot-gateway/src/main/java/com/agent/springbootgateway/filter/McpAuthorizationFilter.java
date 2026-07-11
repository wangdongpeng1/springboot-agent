package com.agent.springbootgateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 认证过滤器
 * 从请求头中提取 X-API-Key 并验证
 */
@Slf4j
@Component
@Order(-1)
public class McpAuthorizationFilter implements GlobalFilter {

    @Value("${mcp.auth.appid}")
    private String mcpAuthAppid;

    @Value("${mcp.auth.appSecret}")
    private String mcpAuthAppSecret;

    @Value("${mcp.auth.switch:true}")
    private Boolean mcpAuthSwitch;


    private static final int IV_LENGTH = 12; // GCM推荐IV长度
    private static final int TAG_LENGTH = 128; // GCM认证标签长度
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String SALT = "wqyzurvru"; // 盐值，可自定义
    private static final String shortKey = "signimqouoewq";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        log.info("MCP Gateway请求拦截 - 路径: {},", path);

        // 检查认证开关
        if (!mcpAuthSwitch) {
            log.info("MCP认证开关未开启，跳过认证直接放行");
            return chain.filter(exchange);
        }

        log.debug("MCP认证开关已开启，开始验证请求");

        // 从 query 参数获取
        String authorization = exchange.getRequest().getQueryParams().getFirst("authorization");
        // 如果 query 中没有，尝试从 header 获取
        if (authorization == null) {
            authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        }

        // 检查是否有授权参数
        if (authorization == null || authorization.isEmpty()) {
            log.warn("MCP认证失败 - 缺少authorization参数");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 解密并验证授权参数
        try {
            log.debug("开始解密authorization参数");
            String decryptedAppSecret = decrypt(authorization, shortKey);

            if (mcpAuthAppSecret.equals(decryptedAppSecret)) {
                log.info("MCP认证成功 - 密钥验证通过");
                return chain.filter(exchange);
            } else {
                log.warn("MCP认证失败 - 解密后的密钥不匹配");
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        } catch (Exception e) {
            log.error("MCP认证失败 - 解密过程出错: {}", e.getMessage(), e);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 从短密码生成加密密钥
     * @param shortPassword 用户提供的短密码
     * @return 安全密钥字节数组
     */
    private byte[] deriveKey(String shortPassword) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        // 加盐处理增强安全性
        String saltedPassword = shortPassword + SALT;
        return digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 加密数据
     * @param plainText 明文数据（16进制字符串）
     * @param shortKey 短密码
     * @return 加密后的Base64编码字符串
     */
    public String encrypt(String plainText, String shortKey) throws Exception {
        // 从短密码生成密钥
        byte[] keyBytes = deriveKey(shortKey);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

        // 生成随机IV
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        // 初始化加密器
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        // 将16进制字符串转换为字节数组
        byte[] plainBytes = hexStringToByteArray(plainText);

        // 执行加密
        byte[] encryptedBytes = cipher.doFinal(plainBytes);

        // 合并IV和密文
        byte[] encryptedWithIv = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, encryptedWithIv, iv.length, encryptedBytes.length);

        // 使用URL安全的Base64编码，不含特殊符号
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedWithIv);
    }

    /**
     * 解密数据
     * @param cipherText 加密后的Base64编码字符串
     * @param shortKey 短密码
     * @return 解密后的16进制字符串
     */
    public String decrypt(String cipherText, String shortKey) throws Exception {
        // 从短密码生成密钥
        byte[] keyBytes = deriveKey(shortKey);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

        // 解码Base64数据
        byte[] encryptedWithIv = Base64.getUrlDecoder().decode(cipherText);

        // 分离IV和密文
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedBytes = new byte[encryptedWithIv.length - IV_LENGTH];

        System.arraycopy(encryptedWithIv, 0, iv, 0, IV_LENGTH);
        System.arraycopy(encryptedWithIv, IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

        // 初始化解密器
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        // 执行解密
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // 转换为16进制字符串
        return byteArrayToHexString(decryptedBytes);
    }

    /**
     * 字节数组转16进制字符串
     */
    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    /**
     * 16进制字符串转字节数组
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

}
