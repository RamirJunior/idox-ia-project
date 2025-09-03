package br.gov.ma.idox.config.upload;

import br.gov.ma.idox.service.UploadDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UploadDirectoryService uploadService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = "file:" + uploadService.getUploadDir();
        registry.addResourceHandler("/uploads/**").addResourceLocations(uploadPath);
    }
}
