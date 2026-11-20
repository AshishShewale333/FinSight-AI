package com.finsight.authentication.api;

import com.finsight.authentication.application.AuthenticationService;
import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                               HttpServletRequest httpRequest) {
        TokenResponse tokens = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Account created", tokens,
                httpRequest.getHeader(CorrelationIdFilter.HEADER_NAME)));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.success("Login successful", authenticationService.login(request),
                httpRequest.getHeader(CorrelationIdFilter.HEADER_NAME));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                              HttpServletRequest httpRequest) {
        return ApiResponse.success("Tokens refreshed", authenticationService.refresh(request),
                httpRequest.getHeader(CorrelationIdFilter.HEADER_NAME));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        authenticationService.logout(request.refreshToken());
        return ApiResponse.success("Logged out", null, httpRequest.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
