package com.collins.platform.accounts.auth;

import com.collins.platform.accounts.auth.AuthenticationService.LoginResult;
import com.collins.platform.accounts.auth.AuthenticationService.UserProfile;
import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @NotBlank String name,
            @NotBlank String role, // ADMIN, MERCHANT, CUSTOMER
            @NotBlank String tenantId
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String userId,
            String email,
            String name,
            String role,
            String tenantId,
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {
    }

    public record UserProfileResponse(
            String id,
            String email,
            String name,
            String role,
            String tenantId
    ) {
    }

    private final AuthenticationService authService;

    public AuthController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody ApiRequest<RegisterRequest> request) {
        RegisterRequest req = request.payload();
        UserProfile profile = authService.register(req.email(), req.password(), req.name(), req.role(), req.tenantId());

        UserProfileResponse response = new UserProfileResponse(
                profile.id(), profile.email(), profile.name(), profile.role(), profile.tenantId()
        );

        return ApiResponse.ok(response);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody ApiRequest<LoginRequest> request) {
        LoginRequest req = request.payload();
        LoginResult result = authService.login(req.email(), req.password());

        LoginResponse response = new LoginResponse(
                result.userId(),
                result.email(),
                result.name(),
                result.role(),
                result.tenantId(),
                result.accessToken(),
                result.refreshToken(),
                result.expiresIn()
        );

        return ApiResponse.ok(response);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getCurrentUser(@RequestHeader("X-User-Id") String userId) {
        UserProfile profile = authService.getProfile(userId);

        UserProfileResponse response = new UserProfileResponse(
                profile.id(), profile.email(), profile.name(), profile.role(), profile.tenantId()
        );

        return ApiResponse.ok(response);
    }
}
