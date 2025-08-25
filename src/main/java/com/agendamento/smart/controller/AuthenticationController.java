package com.agendamento.smart.controller;

import com.agendamento.smart.dtos.AuthenticationDTO;
import com.agendamento.smart.dtos.LoginResponseDTO;
import com.agendamento.smart.dtos.RegisterDTO;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
@RestController
@RequestMapping("auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationDTO data, HttpServletResponse response){
        System.out.println("register");
        var userNamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(userNamePassword);

        var token = tokenService.generateToken( (User) auth.getPrincipal());

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)              // 👈 não acessível via JS
                .secure(true)             // true em produção com HTTPS // false para local com HTTP
                .path("/")
                .maxAge(Duration.ofHours(1)) // tempo de expiração
                .sameSite("None")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        User user = (User) userRepository.findByLogin(data.login());
        System.out.println(user.getRole());
        return ResponseEntity.ok(new LoginResponseDTO(token, user));
    }
    @CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
    @PostMapping("/register")
    public ResponseEntity registrer(@RequestBody @Valid RegisterDTO data){

        if(this.userRepository.findByLogin(data.login()) != null) return ResponseEntity.badRequest().build();
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User newUser = new User(data.login(), encryptedPassword, data.role());
        this.userRepository.save(newUser);

        return ResponseEntity.ok().build();
    }
}
