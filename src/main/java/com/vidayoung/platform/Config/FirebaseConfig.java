package com.vidayoung.platform.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.enabled:false}")
    private boolean enabled;

    @Value("${firebase.service-account.path:classpath:firebase/serviceAccountKey.json}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() throws IOException {
        if (!enabled) {
            System.out.println("⚠️ Firebase deshabilitado en application.properties");
            return;
        }

        ClassPathResource resource = new ClassPathResource(serviceAccountPath);
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream())
                .createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));

        // Inicializar Firebase App
        com.google.firebase.FirebaseApp.initializeApp(
                com.google.firebase.FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build()
        );

        System.out.println("✅ Firebase inicializado correctamente");
        // No necesitar llamar a setAutoInitEnabled en esta versión; el SDK lo maneja automáticamente
        // si está configurado correctamente en la service account key
    }

    public boolean isEnabled() {
        return enabled;
    }
}