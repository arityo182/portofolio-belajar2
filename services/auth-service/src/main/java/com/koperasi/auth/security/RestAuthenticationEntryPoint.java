package com.koperasi.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koperasi.auth.domain.dto.ErrorResponse;
import com.koperasi.auth.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ERROR_CODE_ATTRIBUTE = "security.error.code";
    public static final String ERROR_MESSAGE_ATTRIBUTE = "security.error.message";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ErrorCode code = (ErrorCode) request.getAttribute(ERROR_CODE_ATTRIBUTE);
        String message = (String) request.getAttribute(ERROR_MESSAGE_ATTRIBUTE);
        if (code == null) {
            code = ErrorCode.UNAUTHORIZED;
        }
        if (message == null) {
            message = "Autentikasi diperlukan untuk mengakses resource ini";
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(code, message, List.of()));
    }
}
