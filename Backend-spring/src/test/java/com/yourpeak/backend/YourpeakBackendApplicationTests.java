package com.yourpeak.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.yourpeak.backend.service.RegistroService;
import com.yourpeak.backend.repository.UsuarioRepository;
import com.yourpeak.backend.repository.ClienteRepository;
import com.yourpeak.backend.repository.TipoEstadoRepository;
import com.yourpeak.backend.model.Cliente;
import com.yourpeak.backend.model.Usuario;
import org.springframework.transaction.annotation.Transactional;
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
    @Autowired ClienteRepository clientes;
    @Autowired TipoEstadoRepository estados;

    @Test
    @Transactional
    void listaPendientesExcluyeUsuariosConRolAdministrador() {
        jdbc.update("INSERT INTO tipo_estado(nombre) SELECT 'pendiente' WHERE NOT EXISTS (SELECT 1 FROM tipo_estado WHERE nombre = 'pendiente')");
        var pendiente = estados.findByNombre("pendiente").orElseThrow();
        var admin = usuarios.saveAndFlush(new Usuario("Admin", "Prueba", "admin.consulta@tecsup.edu.pe", "hash", null));
        var usuario = usuarios.saveAndFlush(new Usuario("Cliente", "Prueba", "cliente.consulta@tecsup.edu.pe", "hash", null));
        var perfilAdmin = clientes.saveAndFlush(new Cliente(admin, pendiente));
        var perfilCliente = clientes.saveAndFlush(new Cliente(usuario, pendiente));
        jdbc.update("INSERT INTO administrador(id_usuario) VALUES (?)", admin.getIdUsuario());

        var resultado = clientes.findAllByTipoEstado_NombreIgnoreCaseOrderByFechaAltaAsc("PENDIENTE");
        assertTrue(resultado.stream().anyMatch(item -> item.getIdCliente().equals(perfilCliente.getIdCliente())));
        assertFalse(resultado.stream().anyMatch(item -> item.getIdCliente().equals(perfilAdmin.getIdCliente())));
    }

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
