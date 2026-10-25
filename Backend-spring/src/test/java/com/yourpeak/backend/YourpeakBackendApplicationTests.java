package com.yourpeak.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yourpeak.backend.service.RegistroService;
import com.yourpeak.backend.repository.UsuarioRepository;
import com.yourpeak.backend.dto.RegistroRequest;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:yourpeak;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class YourpeakBackendApplicationTests {

    @Autowired RegistroService registro;
    @Autowired UsuarioRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    @Test
    void registrationPersistsBirthDateAndHashesPassword() {
        jdbc.update("INSERT INTO tipo_estado(nombre) VALUES (?)", "pendiente");
        var birthDate = LocalDate.of(2000, 2, 29);
        var result = registro.registrar(new RegistroRequest("Ana", "Prueba", birthDate,
                "ana@tecsup.edu.pe", "una frase de prueba larga"));
        var saved = usuarios.findById(result.idUsuario()).orElseThrow();
        assertEquals(birthDate, saved.getFechaNacimiento());
        assertNotEquals("una frase de prueba larga", saved.getContrasenaHash());
        assertNotNull(result.idCliente());
        assertEquals("pendiente", result.estado());
    }

	@Test
	void contextLoads() {
	}

}
