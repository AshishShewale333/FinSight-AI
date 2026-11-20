package com.finsight.user.api;

import com.finsight.common.api.ApiResponse;
import com.finsight.common.config.CorrelationIdFilter;
import com.finsight.user.application.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> currentUser(@AuthenticationPrincipal Jwt jwt,
                                                  HttpServletRequest request) {
        return ApiResponse.success("Profile retrieved", userService.getProfile(jwt.getSubject()),
                request.getHeader(CorrelationIdFilter.HEADER_NAME));
    }
}
