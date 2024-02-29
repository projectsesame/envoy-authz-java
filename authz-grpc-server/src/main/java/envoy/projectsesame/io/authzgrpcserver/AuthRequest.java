package envoy.projectsesame.io.authzgrpcserver;

import lombok.Builder;
import lombok.Data;

/**
 * @author yangyang
 * @date 2024/2/26 11:15
 */

@Data
@Builder
public class AuthRequest {
    private String path;
    private String method;
    private final int forgetMatchingLength = 3;
}
