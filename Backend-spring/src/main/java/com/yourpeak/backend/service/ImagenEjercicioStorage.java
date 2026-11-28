package com.yourpeak.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ImagenEjercicioStorage {
    public static final String RUTA = "/api/ejercicios/imagenes";
    private static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_LADO = 2048;
    private static final Pattern ARCHIVO = Pattern.compile("[a-f0-9]{32}\\.(jpg|png)");
    private final Path directorio;

    public ImagenEjercicioStorage(@Value("${yourpeak.ejercicios.imagenes-dir:imagenes-ejercicios}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    public String guardar(String imagenBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(imagenBase64);
        } catch (IllegalArgumentException e) {
            throw imagenInvalida();
        }
        if (bytes.length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen admite hasta 1 MB.");
        }
        String extension = validarImagen(bytes);
        String nombre = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path destino = directorio.resolve(nombre);
        try {
            Files.createDirectories(directorio);
            Path temporal = Files.createTempFile(directorio, "carga-", ".tmp");
            try {
                Files.write(temporal, bytes);
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo guardar la imagen.", e);
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado == STATUS_ROLLED_BACK) {
                    try {
                        Files.deleteIfExists(destino);
                    } catch (IOException e) {
                        throw new IllegalStateException("No se pudo limpiar la carga revertida.", e);
                    }
                }
            }
        });
        return RUTA + "/" + nombre;
    }

    public Resource obtener(String nombre) {
        if (!ARCHIVO.matcher(nombre).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path ruta = directorio.resolve(nombre);
        if (!Files.isRegularFile(ruta)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new FileSystemResource(ruta);
    }

    private static String validarImagen(byte[] bytes) {
        try (var entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw imagenInvalida();
            var lector = lectores.next();
            try {
                String formato = lector.getFormatName().toLowerCase(Locale.ROOT);
                if (!formato.equals("jpeg") && !formato.equals("png")) throw imagenInvalida();
                lector.setInput(entrada, true, true);
                if (lector.getWidth(0) > MAX_LADO || lector.getHeight(0) > MAX_LADO) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen admite hasta 2048 × 2048 píxeles.");
                }
                lector.read(0);
                return formato.equals("jpeg") ? "jpg" : "png";
            } finally {
                lector.dispose();
            }
        } catch (IOException e) {
            throw imagenInvalida();
        }
    }

    private static ResponseStatusException imagenInvalida() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una imagen JPG o PNG válida.");
    }
}
