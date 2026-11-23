package com.ozerler.marble.config;

import com.ozerler.marble.web.FormDraftInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FormDraftInterceptor formDraftInterceptor;
    private final ObjectStorageProperties storageProperties;

    public WebConfig(FormDraftInterceptor formDraftInterceptor, ObjectStorageProperties storageProperties) {
        this.formDraftInterceptor = formDraftInterceptor;
        this.storageProperties = storageProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(formDraftInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path mediaPath = Paths.get(storageProperties.getMediaDir());
        String mediaAbsolutePath = mediaPath.toFile().getAbsolutePath();

        Path uploadPath = Paths.get(storageProperties.getUploadDir());
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
