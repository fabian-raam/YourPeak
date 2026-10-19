package com.yourpeak.backend.controller;

import com.yourpeak.backend.dto.RegistroRequest;
import com.yourpeak.backend.dto.RegistroResponse;
import com.yourpeak.backend.service.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @PostMapping(
            value = "/registro",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<RegistroResponse> registrar(
            @Valid @RequestBody RegistroRequest request
    ) {
        RegistroResponse response = registroService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}