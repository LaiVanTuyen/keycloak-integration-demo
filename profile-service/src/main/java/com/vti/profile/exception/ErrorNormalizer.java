package com.vti.profile.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vti.profile.dto.identity.KeyCloakError;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lớp ErrorNormalizer dùng để chuyển đổi lỗi trả về từ Keycloak (thông qua FeignException)
 * thành AppException với ErrorCode phù hợp cho ứng dụng. Lớp này giúp chuẩn hóa việc xử lý lỗi
 * từ các response của Keycloak, đảm bảo các tầng khác của ứng dụng nhận được lỗi nhất quán.
 */
@Component
@Slf4j
public class ErrorNormalizer {
    // ObjectMapper dùng để parse lỗi JSON trả về từ Keycloak
    private final ObjectMapper objectMapper;
    // Map ánh xạ thông báo lỗi cụ thể của Keycloak sang ErrorCode nội bộ
    private final Map<String, ErrorCode> errorCodeMap;

    /**
     * Khởi tạo ObjectMapper và ánh xạ mã lỗi
     */
    public ErrorNormalizer() {
        objectMapper = new ObjectMapper();
        errorCodeMap = new HashMap<>();

        // Ánh xạ thông báo lỗi của Keycloak sang mã lỗi nội bộ
        errorCodeMap.put("User exists with same username", ErrorCode.USER_EXISTED);
        errorCodeMap.put("User exists with same email", ErrorCode.EMAIL_EXISTED);
        errorCodeMap.put("User name is missing", ErrorCode.USERNAME_IS_MISSING);
    }

    /**
     * Xử lý FeignException trả về từ Keycloak, chuyển thành AppException với ErrorCode phù hợp
     *
     * @param exception FeignException do Keycloak trả về
     * @return AppException với ErrorCode tương ứng
     */
    public AppException handleKeyCloakException(FeignException exception){
        try {
            log.warn("Cannot complete request", exception);
            // Parse lỗi trả về từ Keycloak
            var response = objectMapper.readValue(exception.contentUTF8(), KeyCloakError.class);

            // Nếu thông báo lỗi nằm trong map, trả về AppException với ErrorCode tương ứng
            if (Objects.nonNull(response.getErrorMessage()) &&
                    Objects.nonNull(errorCodeMap.get(response.getErrorMessage()))){
                return new AppException(errorCodeMap.get(response.getErrorMessage()));
            }
        } catch (JsonProcessingException e) {
            log.error("Cannot deserialize content", e);
        }

        // Nếu lỗi không xác định, trả về AppException với mã lỗi không phân loại
        return new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
}
