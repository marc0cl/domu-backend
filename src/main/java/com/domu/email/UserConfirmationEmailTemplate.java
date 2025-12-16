package com.domu.email;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class UserConfirmationEmailTemplate {

    private static final String TEMPLATE_PATH = "/templates/building-request-user-confirmation.html";

    private UserConfirmationEmailTemplate() {
    }

    public static String render(Map<String, String> params) {
        String html = loadTemplate();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return html;
    }

    private static String loadTemplate() {
        try (InputStream is = UserConfirmationEmailTemplate.class.getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new IllegalStateException("No se encontró la plantilla de correo: " + TEMPLATE_PATH);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo la plantilla de correo", e);
        }
    }
}

