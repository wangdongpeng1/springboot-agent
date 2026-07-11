package com.agent.springbootocr.util;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件操作工具类
 */
@Component
public class MinIOUtil {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 上传文件
     * @param file 从Controller接收的MultipartFile
     * @param objectName 在MinIO中存储的文件名（可带路径，如 "images/avatar.jpg"）
     */
    public void uploadFile(MultipartFile file, String objectName) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1) // -1表示不自动分片
                            .contentType(file.getContentType())
                            .build()
            );
        }
    }

    /**
     * 下载文件
     * @param objectName 要下载的文件名
     * @return 文件的输入流
     */
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 删除文件
     *
     * @param objectName 要删除的对象名称
     */
    public void deleteFile(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 生成预签名下载链接（默认有效期 7 天）
     *
     * @param objectName 对象名称
     * @return 预签名 URL
     */
    public String getPresignedUrl(String objectName) throws Exception {
        return getPresignedUrl(objectName, 7, TimeUnit.DAYS);
    }

    /**
     * 生成预签名下载链接（自定义有效期）
     *
     * @param objectName 对象名称
     * @param expiry     过期时间
     * @param timeUnit   时间单位
     * @return 预签名 URL
     */
    public String getPresignedUrl(String objectName, int expiry, TimeUnit timeUnit) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expiry, timeUnit)
                        .build()
        );
    }

    /**
     * 生成预签名上传链接（自定义有效期）
     *
     * @param objectName 对象名称
     * @param expiry     过期时间
     * @param timeUnit   时间单位
     * @return 预签名上传 URL
     */
    public String getPresignedUploadUrl(String objectName, int expiry, TimeUnit timeUnit) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(expiry, timeUnit)
                        .build()
        );
    }

    /**
     * 获取文件的 Base64 编码
     *
     * @param objectName 对象名称
     * @return Base64 编码字符串
     */
    public String getFileAsBase64(String objectName) throws Exception {
        try (InputStream inputStream = downloadFile(objectName);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
             byte[] buffer = new byte[8192];
             int bytesRead;
             while ((bytesRead = inputStream.read(buffer)) != -1) {
                 outputStream.write(buffer, 0, bytesRead);
             }
             return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    /**
     * 获取文件的 Data URI（带 MIME 前缀的 Base64）
     * <p>例如: data:image/png;base64,iVBORw0KGgo...</p>
     *
     * @param objectName  对象名称
     * @param contentType MIME 类型（如 "image/png"）
     * @return Data URI 字符串
     */
    public String getFileAsDataUri(String objectName, String contentType) throws Exception {
        String base64 = getFileAsBase64(objectName);
        return "data:" + contentType + ";base64," + base64;
    }

    /**
     * 判断文件是否存在
     *
     * @param objectName 对象名称
     * @return true 表示存在
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
