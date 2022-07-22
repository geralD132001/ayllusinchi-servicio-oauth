package com.backend.springboot.app.oauth.services;

import brave.Tracer;
import com.backend.springboot.app.commons.usuarios.models.entity.Usuario;
import com.backend.springboot.app.oauth.clients.UsuarioFeignClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuariosService implements UserDetailsService, IUsuarioService {

    private Logger log = LoggerFactory.getLogger(UsuariosService.class);

    @Autowired
    private UsuarioFeignClient client;

    @Autowired
    private Tracer tracer;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        try {
            Usuario usuario = client.findByUsername(username);

            List<GrantedAuthority> authorities = usuario.getRoles()
                    .stream()
                    .map(role -> new SimpleGrantedAuthority(role.getNombre()))
                    .peek(authority -> log.info("Rol: " + authority.getAuthority()))
                    .collect(Collectors.toList());
            log.info("Usuario autenticado" + username);
            return new User(usuario.getUsername(), usuario.getPassword(), usuario.getEstado(), true, true, true, authorities);
        } catch (FeignException e) {
            String error = "Error en el login, no existe usuario'" + username + "' en el sistema";
            log.error(error);
            tracer.currentSpan().tag("error.mensaje", error + ": "+ e.getMessage());
            throw new UsernameNotFoundException(error);
        }
    }

    @Override
    public Usuario findByUsername(String username) {
        return client.findByUsername(username);
    }

    @Override
    public Usuario update(Usuario usuario, Long id) {
        return client.update(usuario, id);
    }
}
