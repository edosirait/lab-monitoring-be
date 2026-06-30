package com.lab.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private Integer id;
    private String username;
    private String name;
    private String role;
    private String token; // Token sesi login (Bisa berupa JWT atau string acak)
}
