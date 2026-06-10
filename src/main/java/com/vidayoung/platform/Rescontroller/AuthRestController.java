package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.Auth.LoginRequest;
import com.vidayoung.platform.Dto.Auth.LoginResponse;
import com.vidayoung.platform.Dto.Auth.ProfileResponse;
import com.vidayoung.platform.Model.Dao.UsuarioDao;
import com.vidayoung.platform.Model.Entity.Usuario;
import com.vidayoung.platform.Model.Service.UsuarioService;
import com.vidayoung.platform.Security.JwtService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Path PROFILE_UPLOAD_DIR = Paths.get("uploads", "perfiles");

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioDao usuarioDao;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioDao.findByUsername(userDetails.getUsername()).orElseThrow();
        String token = jwtService.generarToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(new LoginResponse(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getUsername(),
                roles
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> perfil(@AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = buscarUsuarioAutenticado(userDetails);
        return ResponseEntity.ok(ProfileResponse.desdeUsuario(usuario));
    }

    @PostMapping("/me/foto")
    public ResponseEntity<ProfileResponse> subirFotoPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("foto") MultipartFile foto
    ) throws IOException {
        if (foto.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = foto.getContentType();
        if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        Usuario usuario = buscarUsuarioAutenticado(userDetails);
        Files.createDirectories(PROFILE_UPLOAD_DIR);

        String extension = StringUtils.getFilenameExtension(foto.getOriginalFilename());
        String safeExtension = extension == null ? "jpg" : extension.toLowerCase(Locale.ROOT);
        String fileName = "usuario-" + usuario.getId() + "-" + UUID.randomUUID() + "." + safeExtension;
        Path destino = PROFILE_UPLOAD_DIR.resolve(fileName).normalize();
        foto.transferTo(destino);

        Usuario actualizado = usuarioService.actualizarFotoPerfil(usuario.getId(), "/uploads/perfiles/" + fileName);
        return ResponseEntity.ok(ProfileResponse.desdeUsuario(actualizado));
    }

    private Usuario buscarUsuarioAutenticado(UserDetails userDetails) {
        return usuarioDao.findByUsername(userDetails.getUsername()).orElseThrow();
    }
}
