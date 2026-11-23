package com.agendamento.smart.controller.auth;

import com.agendamento.smart.dtos.AuthenticationDTO;
import com.agendamento.smart.dtos.LoginResponseDTO;
import com.agendamento.smart.dtos.UserLoginResponse;
import com.agendamento.smart.model.user.User;
import com.agendamento.smart.model.user.dto.UserProfileResponse;
import com.agendamento.smart.model.user.dto.UserListResponse;
import com.agendamento.smart.repository.UserRepository;
import com.agendamento.smart.service.AuthSessionService;
import com.agendamento.smart.service.UserService;
import com.agendamento.smart.repository.UnitRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

@CrossOrigin(origins = {
        "https://agendamentos-smart.vercel.app",
        "http://localhost:3000",
        "http://localhost:5173"
})
@RestController
@RequestMapping("auth")
public class AuthenticationController {
    @Value("${api.security.cookie.same-site:Lax}")
    String jwtCookieSameSite;
    @Value("${api.security.cookie.access-name:jwt}")
    String accessCookieName;
    @Value("${api.security.cookie.refresh-name:refresh_token}")
    String refreshCookieName;
    @Value("${api.security.token.access-ttl:15m}")
    Duration accessCookieTtl;
    @Value("${api.security.token.refresh-ttl:30d}")
    Duration refreshCookieTtl;
    /* Esse serviço abaixo esta injetado na classe SecurityConfiguration como @Bean*/
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UnitRepository unitRepository;
    private final AuthSessionService authSessionService;
    private final boolean jwtCookieSecure;

    public AuthenticationController(AuthenticationManager authenticationManager,
                                    UserRepository userRepository,
                                    UserService userService,
                                    UnitRepository unitRepository,
                                    AuthSessionService authSessionService,
                                    @Value("${api.security.cookie.secure:false}") boolean jwtCookieSecure) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userService = userService;
        this.unitRepository = unitRepository;
        this.authSessionService = authSessionService;
        this.jwtCookieSecure = jwtCookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data,
                                                  HttpServletResponse response){
        /*Cria um objeto do tipo UsernamePasswordAuthenticationToken que é necessário para authenticar*/
        var userNamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());

        /*authenticationManager, responsável pela Autenticação e retorna um objeto do tipo Authenticatio*/
        var auth = this.authenticationManager.authenticate(userNamePassword);

        User user = (User) auth.getPrincipal();
        AuthSessionService.SessionTokens tokens = authSessionService.create(user);
        addSessionCookies(response, tokens);
        return ResponseEntity.ok(new LoginResponseDTO(new UserLoginResponse(
                user.getId(),
                user.getLogin(),
                user.getRole(),
                user.getClinic().getId(),
                user.getClinicUnit().getId())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieValue(request, refreshCookieName);
        try {
            AuthSessionService.SessionTokens tokens = authSessionService.refresh(refreshToken);
            addSessionCookies(response, tokens);
            return ResponseEntity.noContent().build();
        } catch (AuthSessionService.InvalidSessionException exception) {
            clearSessionCookies(response);
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authSessionService.logout(cookieValue(request, refreshCookieName));
        clearSessionCookies(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserListResponse>> list(
            @RequestParam(required = false) UUID unitId,
            Authentication authentication) {
        User principal = (User) authentication.getPrincipal();
        List<User> users;
        if (unitId == null) {
            users = userRepository.findAllByClinicId(principal.getClinic().getId());
        } else {
            unitRepository.findByIdAndClinicId(unitId, principal.getClinic().getId())
                    .orElseThrow(com.agendamento.smart.controller.exception.DomainException::tenantResourceNotFound);
            users = userRepository.findAllByClinicIdAndClinicUnitId(
                    principal.getClinic().getId(), unitId);
        }
        return ResponseEntity.ok(users.stream().map(this::toListResponse).toList());
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Authentication authentication){
        UserProfileResponse profile = userService.getUserProfile(authentication);
        return ResponseEntity.ok(profile);
    }

    private UserListResponse toListResponse(User user) {
        return new UserListResponse(
                user.getId(),
                user.getLogin(),
                user.getRole(),
                user.getClinic().getId(),
                user.getClinicUnit() == null ? null : user.getClinicUnit().getId(),
                user.getClinicUnit() == null ? null : user.getClinicUnit().getName());
    }

    private void addSessionCookies(
            HttpServletResponse response,
            AuthSessionService.SessionTokens tokens) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(
                accessCookieName, tokens.accessToken(), accessCookieTtl).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(
                refreshCookieName, tokens.refreshToken(), refreshCookieTtl).toString());
    }

    private void clearSessionCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(
                accessCookieName, "", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(
                refreshCookieName, "", Duration.ZERO).toString());
    }

    private ResponseCookie sessionCookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(jwtCookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(jwtCookieSameSite)
                .build();
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
