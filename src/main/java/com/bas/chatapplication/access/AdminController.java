package com.bas.chatapplication.access;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin")
public class AdminController {

    @GetMapping("/verify-jwt-token")
    public ResponseEntity<String> verifyToken() {
        return ResponseEntity.ok("Verification successful.");
    }

    @GetMapping
    public String get() {
        return "GET request successful from admin controller.";
    }

    @PostMapping
    public String post() {
        return "POST request successful from admin controller.";
    }

    @PutMapping
    public String put() {
        return "PUT request successful from admin controller.";
    }

    @DeleteMapping
    public String delete() {
        return "DELETE request successful from admin controller.";
    }
}
