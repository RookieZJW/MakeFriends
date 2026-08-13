package com.makefriends.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath = System.getProperty("user.dir") + "/uploads/";
        // 新路径 /files/** （UploadController 返回的 url-prefix）
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + absPath);
        // 兼容旧路径 /upload/** （数据库历史数据 /upload/avatar/xxx.jpg）
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + absPath);
    }
}
