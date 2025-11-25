package com.example.socialmedia.controller;

import com.example.socialmedia.annotation.RequireUserRole;
import com.example.socialmedia.util.ResponseUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class FileUploadController {

    private final Path fileStorageLocation;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".pdf", ".doc", ".docx", ".xls", ".xlsx"
    ));

    public FileUploadController() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @PostMapping
    @RequireUserRole
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        // Validate file is not empty
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse("File is empty"));
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse("File size exceeds maximum allowed size of 10MB"));
        }

        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName == null || originalFileName.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse("Invalid filename"));
        }

        String fileExtension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            fileExtension = originalFileName.substring(i).toLowerCase();
        }

        // Validate file extension
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            return ResponseEntity.badRequest()
                    .body(ResponseUtil.buildErrorResponse("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS));
        }

        // Generate new filename to avoid conflicts
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                return ResponseEntity.badRequest()
                        .body(ResponseUtil.buildErrorResponse("Invalid filename"));
            }

            // Copy file to the target location
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return the download URL
            String fileDownloadUri = "/api/uploads/files/" + fileName;
            return ResponseEntity.ok(ResponseUtil.buildSuccessResponse(fileDownloadUri));
        } catch (IOException ex) {
            return ResponseEntity.internalServerError()
                    .body(ResponseUtil.buildErrorResponse("Could not store file. Please try again"));
        }
    }

    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<?> downloadFile(@PathVariable String fileName) {
        try {
            // Security: Prevent path traversal
            if (fileName.contains("..")) {
                return ResponseEntity.badRequest()
                        .body(ResponseUtil.buildErrorResponse("Invalid filename"));
            }

            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            
            // Verify file is within storage directory
            if (!filePath.getParent().equals(this.fileStorageLocation)) {
                return ResponseEntity.badRequest()
                        .body(ResponseUtil.buildErrorResponse("Invalid file path"));
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
