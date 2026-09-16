package com.ozerler.marble.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.media.dir:media}")
    private String mediaDir;

    @Value("${app.upload.dir:media}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path mediaPath = Paths.get(mediaDir);
        String mediaAbsolutePath = mediaPath.toFile().getAbsolutePath();

        Path uploadPath = Paths.get(uploadDir);
        String uploadAbsolutePath = uploadPath.toFile().getAbsolutePath();

        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:" + mediaAbsolutePath + "/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadAbsolutePath + "/", "file:" + mediaAbsolutePath + "/");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new LenientIntegerConverter());
    }
}
