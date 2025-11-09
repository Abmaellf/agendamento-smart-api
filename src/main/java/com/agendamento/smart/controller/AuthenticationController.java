package com.agendamento.smart.controller;

import com.agendamento.smart.dtos.AuthenticationDTO;
import com.agendamento.smart.dtos.LoginResponseDTO;
import com.agendamento.smart.dtos.user.RegisterDTO;
import com.agendamento.smart.dtos.user.UserResponseDTO;
import com.agendamento.smart.infra.security.TokenService;
import com.agendamento.smart.mapper.UserMapper;
import com.agendamento.smart.model.clinic.Clinic;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.repository.ClinicRepository;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final UserService userService;

    @CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data,
                                                  HttpServletResponse response){
        var userNamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(userNamePassword);

        var token = tokenService.generateToken( (User) auth.getPrincipal());

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)              // 👈 não acessível via JS
                .secure(false)             // true em produção com HTTPS // false para local com HTTP
                .path("/")
                .maxAge(Duration.ofHours(1)) // tempo de expiração
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        User user = (User) userRepository.findByLogin(data.login());
        System.out.println("Login");
        System.out.println(user.getRole());
        return ResponseEntity.ok(new LoginResponseDTO(token, user));
    }

    @CrossOrigin(origins = "https://agendamentos-smart.vercel.app")
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@RequestBody @Valid RegisterDTO data){
        if (userService.existsByLogin(data.login())) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        Clinic clinic = clinicRepository.findByUuid(data.clinicId().toString())
                .orElseThrow(() -> new RuntimeException("Clinic not found with id: " + data.clinicId()));

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User user = userMapper.toEntity(data, clinic);
        user.setLogin(data.login());
        user.setPassword(encryptedPassword);
        user.setRole(data.role());
        user.setClinic(clinic);
        System.out.println("Register");
         userRepository.save(user);
        return ResponseEntity.ok().build();
    }
}
