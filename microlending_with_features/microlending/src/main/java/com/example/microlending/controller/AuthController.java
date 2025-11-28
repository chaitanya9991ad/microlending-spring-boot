package com.example.microlending.controller;

import com.example.microlending.entity.User;
import com.example.microlending.repository.UserRepository;
import com.example.microlending.security.JwtUtil;
import com.example.microlending.service.EmailService;
import com.example.microlending.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
// --- NEW: Import PasswordEncoder ---
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService,
                          EmailService emailService,
                          UserRepository userRepository,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    //  Registration
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User u = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .username(request.getUsername())
                // --- FIX: Encrypt the password before saving ---
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole().equalsIgnoreCase("ADMIN") ? "ADMIN" : "USER")
                .verified(true)
                .build();

        userService.save(u);
        return ResponseEntity.ok("Registration successful. You can now login via OTP.");
    }

    //  Request OTP
    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestParam String email) {
        // ... existing code ...
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();

        // Generate 6-digit OTP
        String otp = String.valueOf((int) ((Math.random() * 900000) + 100000));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userService.save(user);

        // Send email
        String subject = "Your OTP Code";
        String message = "<p>Dear " + user.getName() + ",</p>"
                + "<p>Your OTP is: <b>" + otp + "</b></p>"
                + "<p>It is valid for 5 minutes.</p>";
        emailService.sendHtmlEmail(user.getEmail(), subject, message);

        return ResponseEntity.ok("OTP sent to your email.");
    }

    //  Login with OTP
    @PostMapping("/login-otp")
    public ResponseEntity<?> loginWithOtp(@RequestParam String email, @RequestParam String otp) {
        // ... existing code ...
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid email or OTP");
        }

        User user = userOpt.get();

        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            return ResponseEntity.status(401).body("No OTP requested");
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            return ResponseEntity.status(401).body("OTP expired");
        }

        if (!otp.equals(user.getOtp())) {
            return ResponseEntity.status(401).body("Invalid OTP");
        }

        //  Clear OTP after success
        user.setOtp(null);
        user.setOtpExpiry(null);
        userService.save(user);

        //  Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(new LoginResponse(token, user.getName(),user.getUserid(), user.getEmail(), user.getRole()));
    }

    // DTOs
    public static class RegisterRequest {
        // ... existing code ...
        private String name;
        private String email;
        private String phone;
        private String password;
        private String role;
        private String username;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    public static class LoginResponse {

        private String token;
        private String name;
        private Long userId;
        private String email;
        private String role;

        public LoginResponse(String token, String name, Long userId, String email, String role) {
            this.token = token;
            this.name = name;
            this.userId = userId;
            this.email = email;
            this.role = role;
        }

        public String getToken() { return token; }
        public String getName() { return name; }
        public Long getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }
}
