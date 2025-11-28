package com.example.microlending.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userid;

    @NotBlank
    private String name;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    @NotBlank
    private String phone;

    @NotBlank
    private String password;

    private String role = "USER";

    private boolean verified = true;

    private String otp;
    private LocalDateTime otpExpiry;



    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Loan> loans;

    public Long getUserid() {
        return userid;
    }

    public void setLoans(List<Loan> loans) {
        this.loans = loans;
    }
}