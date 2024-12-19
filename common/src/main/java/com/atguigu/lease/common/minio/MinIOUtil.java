package com.atguigu.lease.common.minio;

import io.minio.*;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
@Component
public class MinIOUtil {
    // MinIO配置
//    @Value("${minio.endpoint}")
    @Value("${minio.endpoint}")
    private String ENDPOINT;
    @Value("${minio.access-key}")
    private String ACCESS_KEY;
    @Value("${minio.secret-key}")
    private String SECRET_KEY;
    @Value("${minio.bucket-name}")
    private String BUCKET_NAME;
    public String upload(String objectName, MultipartFile file) {
        try {
            // 创建 MinIO 客户端
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(ENDPOINT)
                    .credentials(ACCESS_KEY, SECRET_KEY)
                    .build();

            // 检查桶是否存在，不存在则创建
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());

                // 设置公共访问策略
                String policy = """
                        {
                          "Statement" : [ {
                            "Action" : "s3:GetObject",
                            "Effect" : "Allow",
                            "Principal" : "*",
                            "Resource" : "arn:aws:s3:::%s/*"
                          } ],
                          "Version" : "2012-10-17"
                        }""".formatted(BUCKET_NAME);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(BUCKET_NAME).config(policy).build());
            }

            // 获取文件流和元信息
            InputStream inputStream = file.getInputStream();
            long size = file.getSize();
            String contentType = file.getContentType();

            // 上传文件到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(inputStream, size, -1)  // -1 表示未知大小
                            .contentType(contentType)
                            .build()
            );

            // 返回文件的访问 URL
            return ENDPOINT + "/" + BUCKET_NAME + "/" + objectName;

        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return "上传失败: " + e.getMessage();
        }
    }
}
