package com.yourpeak.backend;

import com.yourpeak.backend.dto.LoginRequest;
import com.yourpeak.backend.dto.RegistroRequest;
import com.yourpeak.backend.model.Usuario;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class AuthValidationTests {
    @Test
    void onlyExactInstitutionalDomainIsAccepted() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new LoginRequest(" ANA@TECSUP.EDU.PE ", "secret")).isEmpty());
            for (String email : new String[]{"ana@gmail.com", "ana@sub.tecsup.edu.pe", "ana@tecsup.edu.pe.evil.com", "ana@@tecsup.edu.pe"}) {
                assertFalse(validator.validate(new LoginRequest(email, "secret")).isEmpty(), email);
            }
        }
    }

    @Test
    void registrationRequiresPastBirthDateAndInstitutionalEmail() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(registration(LocalDate.of(2000, 2, 29), "ana@tecsup.edu.pe")).isEmpty());
            assertFalse(validator.validate(registration(null, "ana@tecsup.edu.pe")).isEmpty());
            assertFalse(validator.validate(registration(LocalDate.now().plusDays(1), "ana@tecsup.edu.pe")).isEmpty());
            assertFalse(validator.validate(registration(LocalDate.of(2000, 1, 1), "ana@gmail.com")).isEmpty());
        }
    }

    @Test
    void userRetainsBirthDate() {
        LocalDate birthDate = LocalDate.of(2000, 2, 29);
        var user = new Usuario("Ana", "Perez", "ana@tecsup.edu.pe", "hash", birthDate);
        assertEquals(birthDate, user.getFechaNacimiento());
    }

    private RegistroRequest registration(LocalDate date, String email) {
        return new RegistroRequest("Ana", "Perez", date, email, "una frase de prueba larga");
    }
}
