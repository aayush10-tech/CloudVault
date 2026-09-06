package com.cloudstorage.controller;
import org.springframework.web.server.ResponseStatusException;
import com.cloudstorage.model.User; import com.cloudstorage.repository.UserRepository; import com.cloudstorage.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*; import org.springframework.http.*; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
 public AuthController(UserRepository u,PasswordEncoder e,JwtService j){users=u;encoder=e;jwt=j;}
 public record AuthRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password
    ){}
 public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String name
    ){}
 @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody RegisterRequest r){
  if(users.findByEmail(r.email()).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already registered");
  User u=new User(); u.setEmail(r.email().toLowerCase());u.setName(r.name());u.setPasswordHash(encoder.encode(r.password()));users.save(u);
  return Map.of("token",jwt.generate(u.getEmail()),"user",Map.of("id",u.getId(),"name",u.getName(),"email",u.getEmail()));
 }
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody AuthRequest r){
  User u=users.findByEmail(r.email().toLowerCase()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"));
  if(!encoder.matches(r.password(),u.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
  return Map.of("token",jwt.generate(u.getEmail()),"user",Map.of("id",u.getId(),"name",u.getName(),"email",u.getEmail()));
 }
 @GetMapping("/me") public User me(@org.springframework.security.core.annotation.AuthenticationPrincipal User u){return u;}
}
