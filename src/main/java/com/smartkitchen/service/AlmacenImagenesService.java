package com.smartkitchen.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * Guarda imágenes de recetas subidas por el usuario en una carpeta local
 * (por defecto ./data/uploads) que se sirve en /uploads/**.
 *
 * Módulo aislado: si no se sube archivo, la app funciona igual con URLs.
 */
@Service
public class AlmacenImagenesService {

    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_BYTES = 8L * 1024 * 1024;

    private final Path carpeta;

    public AlmacenImagenesService(@Value("${smartkitchen.uploads.dir:./data/uploads}") String dir) {
        this.carpeta = Paths.get(dir).toAbsolutePath().normalize();
    }

    /**
     * Guarda el archivo y devuelve la ruta pública (/uploads/xxx.jpg).
     * Devuelve null si el archivo viene vacío.
     */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo de 8 MB.");
        }
        String extension = extension(archivo.getOriginalFilename());
        if (!EXTENSIONES.contains(extension)) {
            throw new IllegalArgumentException("Formato de imagen no admitido. Usa JPG, PNG, WEBP o GIF.");
        }
        try {
            Files.createDirectories(carpeta);
            String nombre = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path destino = carpeta.resolve(nombre).normalize();
            if (!destino.startsWith(carpeta)) {
                throw new IllegalArgumentException("Ruta de archivo no válida.");
            }
            archivo.transferTo(destino);
            return "/uploads/" + nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    public Path getCarpeta() {
        return carpeta;
    }

    private static String extension(String nombreOriginal) {
        String limpio = StringUtils.getFilenameExtension(
                StringUtils.cleanPath(nombreOriginal == null ? "" : nombreOriginal));
        return limpio == null ? "" : limpio.toLowerCase();
    }
}
