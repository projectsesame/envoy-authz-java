package envoy.projectsesame.io.authzgrpcserver;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.HeaderValue;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.*;
import io.envoyproxy.envoy.type.v3.HttpStatus;
import io.envoyproxy.envoy.type.v3.StatusCode;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Collections;


/**
 * @author yangyang
 * @date 2022/12/6 下午4:45
 */
@GrpcService()
public class AuthzService extends AuthorizationGrpc.AuthorizationImplBase {
    private static final String AUTH_URL = "http://rbac-asw-authorization.rbac-aswatson-uat:8080/admin/user/sso";

    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        CheckResponse.Builder checkResponse = CheckResponse.newBuilder();

        WebClient.create()
                .post()
                .uri(AUTH_URL)
                .body(Mono.just(AuthRequest.builder().path(request.getAttributes().getRequest().getHttp().getPath()).method(request.getAttributes().getRequest().getHttp().getMethod()).build()), AuthRequest.class)
                .headers(httpHeaders -> {
                    request.getAttributes().getRequest().getHttp().getHeadersMap().forEach((k, v) -> {
                        if (!k.startsWith(":")){
                            httpHeaders.add(k, v);
                        }
                    });
                    httpHeaders.put("content-type", Collections.singletonList("application/json"));
                })
                .exchangeToMono(res -> {
                    if (res.statusCode().is2xxSuccessful()) {
                        System.out.println("success");

                        checkResponse.setOkResponse(OkHttpResponse.newBuilder()
                                        .addHeaders(HeaderValueOption.newBuilder().setAppendAction(HeaderValueOption.HeaderAppendAction.OVERWRITE_IF_EXISTS_OR_ADD).setHeader(HeaderValue.newBuilder().setKey("userid").setValue(res.headers().header("userid").get(0))).build())
                                        .addHeaders(HeaderValueOption.newBuilder().setAppendAction(HeaderValueOption.HeaderAppendAction.OVERWRITE_IF_EXISTS_OR_ADD).setHeader(HeaderValue.newBuilder().setKey("username").setValue(res.headers().header("username").get(0))).build())
                                        .build())
                                     .setStatus(Status.newBuilder().setCode(Code.OK_VALUE));
                    } else {
                        System.out.println("no-success");
                        return res.bodyToMono(String.class)
                                .doOnNext(body -> {
                                    checkResponse.setDeniedResponse(DeniedHttpResponse.newBuilder()
                                                    .setStatus(HttpStatus.newBuilder().setCodeValue(res.rawStatusCode()).build())
                                                    .setBody(body)
                                                    .build())
                                            .setStatus(Status.newBuilder().setCode(Code.PERMISSION_DENIED_VALUE));
                                });

                    }
                    return Mono.empty();
                })
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(t -> {
                    System.out.println("error");
                    checkResponse.setDeniedResponse(DeniedHttpResponse.newBuilder()
                                    .setStatus(HttpStatus.newBuilder().setCode(StatusCode.Forbidden).build())
                                    .setBody(t.getMessage())
                                    .build())
                            .setStatus(Status.newBuilder().setCode(Code.INTERNAL_VALUE));
                    return Mono.empty();
                })

                .doOnNext(body -> {
                    responseObserver.onNext(checkResponse.build());
                    System.out.println("finish");
                    responseObserver.onCompleted();
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            responseObserver.onNext(checkResponse.build());
                            System.out.println("empty");
                            responseObserver.onCompleted();
                            return Mono.empty();
                        })
                )
                .subscribe();


    }
}
