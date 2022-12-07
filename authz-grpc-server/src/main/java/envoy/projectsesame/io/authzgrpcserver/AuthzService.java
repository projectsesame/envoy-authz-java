package envoy.projectsesame.io.authzgrpcserver;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.envoyproxy.envoy.service.auth.v3.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * @author yangyang
 * @date 2022/12/6 下午4:45
 */
@GrpcService()
public class AuthzService extends AuthorizationGrpc.AuthorizationImplBase {
    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        CheckResponse checkResponse;
        if ("/".equals(request.getAttributes().getRequest().getHttp().getPath())){
            checkResponse = CheckResponse.newBuilder().setOkResponse(OkHttpResponse.getDefaultInstance()).setStatus(Status.newBuilder().setCode(Code.OK_VALUE)).build();
        }else {
            checkResponse = CheckResponse.newBuilder().setDeniedResponse(DeniedHttpResponse.newBuilder().setBody("No permission\n")).setStatus(Status.newBuilder().setCode(Code.PERMISSION_DENIED_VALUE).build()).build();
        }
        responseObserver.onNext(checkResponse);
        responseObserver.onCompleted();
    }
}
