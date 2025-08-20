package com.vti.profile.service;

import com.vti.profile.dto.identity.Credential;
import com.vti.profile.dto.identity.TokenExchangeParam;
import com.vti.profile.dto.identity.UserCreationParam;
import com.vti.profile.dto.request.RegistrationRequest;
import com.vti.profile.dto.response.ProfileResponse;
import com.vti.profile.exception.ErrorNormalizer;
import com.vti.profile.mapper.ProfileMapper;
import com.vti.profile.repository.IdentityClient;
import com.vti.profile.repository.ProfileRepository;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileService {
    ProfileRepository profileRepository;
    ProfileMapper profileMapper;
    IdentityClient identityClient;
    ErrorNormalizer errorNormalizer;

    @Value("${idp.client-id}")
    @NonFinal
    String clientId;

    @Value("${idp.client-secret}")
    @NonFinal
    String clientSecret;

    public List<ProfileResponse> getAllProfiles() {
        var profiles = profileRepository.findAll();
        return profiles.stream().map(profileMapper::toProfileResponse).toList();
    }

    public ProfileResponse register(RegistrationRequest request) {
        try {
            // Create account in keycloak
            // Exchange client token
            var token = identityClient.exchangeToken(
                    TokenExchangeParam.builder()
                            .grant_type("client_credentials")
                            .client_id(clientId)
                            .client_secret(clientSecret)
                            .scope("openid")
                            .build()
            );

            log.info("token: {}", token);
            // Create user with client token and user creation param
            var creationResponse = identityClient.createUser(
                    "Bearer " + token.getAccessToken(),
                    UserCreationParam.builder()
                            .username(request.getUsername())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .email(request.getEmail())
                            .enabled(true)
                            .emailVerified(false)
                            .credentials(List.of(
                                    Credential.builder()
                                            .type("password")
                                            .value(request.getPassword())
                                            .temporary(false)
                                            .build()
                            ))
                            .build()
            );

            // Get userId of keycloak account

            String userId = extractUserId(creationResponse);
            log.info("userId: {}", userId);


            var profile = profileMapper.toProfile(request);
            profile.setUserId(userId);
            profile = profileRepository.save(profile);

            return profileMapper.toProfileResponse(profile);
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }
    /**
     * Extracts the user ID from the Location header of the response.
     *
     * @param response the response entity containing the Location header
     * @return the extracted user ID
     * @throws IllegalArgumentException if the Location header is missing or malformed
     */
    private String extractUserId(ResponseEntity<?> response) {
        return Optional.ofNullable(response.getHeaders().getFirst("Location"))
                .map(location -> location.substring(location.lastIndexOf('/') + 1))
                .orElseThrow(() -> new IllegalArgumentException("Location header is missing"));
    }
}
