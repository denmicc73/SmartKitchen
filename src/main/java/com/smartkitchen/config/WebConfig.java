package com.smartkitchen.config;

import com.smartkitchen.service.AlmacenImagenesService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sirve las imágenes de recetas subidas localmente desde /uploads/**. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AlmacenImagenesService almacenImagenes;

    public WebConfig(AlmacenImagenesService almacenImagenes) {
        this.almacenImagenes = almacenImagenes;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = almacenImagenes.getCarpeta().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(ubicacion)
                .setCachePeriod(3600);
    }
}
