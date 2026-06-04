package com.basketball.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectRoot = Paths.get("").toAbsolutePath().toString();
        String frontendPath = projectRoot + "/frontend";
        
        registry.addResourceHandler("/css/**")
                .addResourceLocations("file:" + frontendPath + "/css/");
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("file:" + frontendPath + "/js/");
        
        registry.addResourceHandler("/pages/**")
                .addResourceLocations("file:" + frontendPath + "/pages/");
        
        
        registry.addResourceHandler("/IMG/**")
                .addResourceLocations("classpath:/static/IMG/");
        
        
        registry.addResourceHandler("/*.html")
                .addResourceLocations("file:" + frontendPath + "/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/pages/login.html");
        registry.addViewController("/login").setViewName("forward:/pages/login.html");
        registry.addViewController("/forgot-password").setViewName("forward:/pages/forgot-password.html");
        registry.addViewController("/dashboard").setViewName("forward:/pages/dashboard.html");
    }
}

