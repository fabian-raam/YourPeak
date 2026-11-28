package com.yourpeak.backend.controller;

import com.yourpeak.backend.service.ImagenEjercicioStorage;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class ImagenEjercicioController {
    private final ImagenEjercicioStorage imagenes;

    public ImagenEjercicioController(ImagenEjercicioStorage imagenes) {
        this.imagenes = imagenes;
    }

    @GetMapping(ImagenEjercicioStorage.RUTA + "/{nombre}")
    public ResponseEntity<Resource> obtener(@PathVariable String nombre) {
        Resource imagen = imagenes.obtener(nombre);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .contentType(nombre.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .body(imagen);
    }
}
