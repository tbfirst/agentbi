package com.tbfirst.agentbiinit.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.qcloud.cos.utils.IOUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * 阿里云 OSS 工具类，该类只提供方法，并未注册到容器中
 * 为什么不用 @Component 直接标记 AliOssUtils？
 * AliOssUtils 需要 4个构造参数，这些参数来自 AliOssProperties，Spring 无法自动装配
 * 因此，需要通过 AliOssConfig 配置类手动创建一个 AliOssUtils Bean 并注册到容器中
 */
@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtils {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传（适用小文件）
     *
     * @param bytes 文件字节数组
     * @param objectName OSS中的文件名（可包含路径，如：images/2024/photo.jpg）
     * @return 文件访问的 URL
     */
    public String uploadFile(byte[] bytes, String objectName) {
        // 1、创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        if(ossClient == null) {
            throw new RuntimeException("将application.yml中的aliyun.oss配置替换为自己的配置，包括endpoint、access-key-id、access-key-secret、bucket-name");
        }

        try {
            // 2、创建 PutObject请求
            // 第一个参数为 Bucket 名称，第二个参数为文件在 OSS 中的路径（包含文件名），第三个参数为文件输入流
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
            log.info("文件上传成功: {}", objectName);
        } catch (OSSException oe) {
            log.error("OSS服务异常: {}", oe.getErrorMessage());
            throw new RuntimeException("OSS服务异常: " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            log.error("OSS客户端异常: {}", ce.getMessage());
            throw new RuntimeException("OSS客户端异常: " + ce.getMessage(), ce);
        } finally {
            ossClient.shutdown();
        }

        // 3、构建文件访问 URL
        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        String url = String.format("https://%s.%s/%s", bucketName, endpoint, objectName);
        log.info("文件访问路径: {}", url);

        return url;
    }
    /**
     * 文件上传（适用大文件，流式上传不占用大量内存）
     *
     * @param inputStream 文件输入流
     * @param objectName OSS中的文件名（可包含路径，如：images/2024/photo.jpg）
     * @return 文件访问的 URL
     */
    public String uploadFileStream(InputStream inputStream, String objectName) {
        // 1、创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 2、创建 PutObject请求
            // 第一个参数为 Bucket 名称，第二个参数为文件在 OSS 中的路径（包含文件名），第三个参数为文件输入流
            ossClient.putObject(bucketName, objectName, inputStream);
            log.info("文件上传成功: {}", objectName);
        } catch (OSSException oe) {
            log.error("OSS服务异常: {}", oe.getErrorMessage());
            throw new RuntimeException("OSS服务异常: " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            log.error("OSS客户端异常: {}", ce.getMessage());
            throw new RuntimeException("OSS客户端异常: " + ce.getMessage(), ce);
        } finally {
            ossClient.shutdown();
        }

        // 3、构建文件访问 URL
        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        String url = String.format("https://%s.%s/%s", bucketName, endpoint, objectName);
        log.info("文件访问路径: {}", url);

        return url;
    }

    /**
     * 从完整 OSS URL 中提取 objectName
     *
     * @param ossUrl 完整 URL，如 https://bucket.endpoint/path/to/file.xlsx
     * @return objectName，如 path/to/file.xlsx
     */
    public static String extractObjectNameFromUrl(String ossUrl) {
        try {
            URL url = new URL(ossUrl);
            String path = url.getPath();
            // 去掉开头的 /
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("无效的 OSS URL: " + ossUrl, e);
        }
    }

    /**
     * 下载文件为字节数组（适合小文件 < 10MB）
     */
    public byte[] downloadFileBytes(String objectName) {
        // 1、创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // 2、下载文件，本质是将文件内容读取到内存中的字节数组
            return IOUtils.toByteArray(ossClient.getObject(bucketName, objectName).getObjectContent());
        } catch (IOException e) {
            throw new RuntimeException("下载文件失败: " + objectName, e);
        } finally {
            // 3、关闭 OSS 客户端
            ossClient.shutdown();
        }
    }

    /**
     * 删除文件
     */
    public void delete(String objectName) {
        // 1、创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // 2、删除文件
            ossClient.deleteObject(bucketName, objectName);
            log.info("文件删除成功: {}", objectName);
        } finally {
            ossClient.shutdown();
        }
    }
}