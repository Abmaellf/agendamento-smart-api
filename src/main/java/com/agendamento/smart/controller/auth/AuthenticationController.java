package com.agendamento.smart.controller.auth;

import com.agendamento.smart.dtos.AuthenticationDTO;
import com.agendamento.smart.dtos.LoginResponseDTO;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

//@CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
@CrossOrigin(origins = "http:localhost:3000")
@RestController
@RequestMapping("auth")
@AllArgsConstructor
public class AuthenticationController {

    /* Esse serviço abaixo esta injetado na classe SecurityConfiguration como @Bean*/
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final UserService userService;

//    @CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
    @CrossOrigin(origins = "http:localhost:3000")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data,
                                                  HttpServletResponse response){
        /*Cria um objeto do tipo UsernamePasswordAuthenticationToken que é necessário para authenticar*/
        var userNamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());

        /*authenticationManager, responsável pela Autenticação e retorna um objeto do tipo Authenticatio*/
        var auth = this.authenticationManager.authenticate(userNamePassword);

        var token = tokenService.generateToken( (User) auth.getPrincipal());

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)              // 👈 não acessível via JS
                .secure(false)             // true em produção com HTTPS // false para local com HTTP
                .path("/")
                .maxAge(Duration.ofHours(1)) // tempo de expiração
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        User user = (User) userRepository.findByLogin(data.login());
        System.out.println("Login");
        System.out.println(user.getRole());
        return ResponseEntity.ok(new LoginResponseDTO(token, user));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<User>> list(){
        List<User> user = userRepository.findAll();
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(Authentication authentication){
        User user = userService.currentUserService(authentication);
        return ResponseEntity.ok(user);
    }
}
