package com.yourpeak.backend.repository;

import com.yourpeak.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreo(String correo);

    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByGoogleSubject(String googleSubject);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Usuario u set u.googleSubject = :subject
            where u.idUsuario = :id and u.correo = :correo and u.googleSubject is null
            """)
    int vincularGoogle(@Param("id") Long id, @Param("correo") String correo, @Param("subject") String subject);
}
