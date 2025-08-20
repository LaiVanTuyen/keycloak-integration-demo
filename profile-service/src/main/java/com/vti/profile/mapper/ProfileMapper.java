package com.vti.profile.mapper;

import com.vti.profile.dto.request.RegistrationRequest;
import com.vti.profile.dto.response.ProfileResponse;
import com.vti.profile.entity.Profile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    Profile toProfile(RegistrationRequest request);
    ProfileResponse toProfileResponse(Profile profile);
}
