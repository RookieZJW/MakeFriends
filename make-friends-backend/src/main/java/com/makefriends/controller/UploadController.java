package com.makefriends.controller;

import com.makefriends.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.url-prefix}")
    private String urlPrefix;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String suffix = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            suffix = originalFilename.substring(dotIndex);
        }
        String allowedSuffixes = ".jpg.jpeg.png.gif.bmp.webp";
        if (!allowedSuffixes.contains(suffix.toLowerCase())) {
            throw new IllegalArgumentException("仅支持图片格式: jpg, jpeg, png, gif, bmp, webp");
        }

        String newFilename = UUID.randomUUID().toString().replace("-", "") + suffix;
        File dir = new File(uploadPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("创建上传目录失败");
        }
        File destFile = new File(dir, newFilename);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", urlPrefix + newFilename);
        result.put("filename", newFilename);
        return Result.ok(result);
    }
}
