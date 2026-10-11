package com.luoyilin.focusguardserver.service;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TencentWechatGatewayTest {
    @SuppressWarnings("unchecked") private HttpResponse<String> response(String body){HttpResponse<String> r=mock(HttpResponse.class);when(r.statusCode()).thenReturn(200);when(r.body()).thenReturn(body);return r;}
    private TencentWechatGateway gateway(HttpClient client){return new TencentWechatGateway("wxbc866bf990e09da5","test-secret-not-a-real-credential",client);}
    @Test void validCodeUsesFixedWechatHttpsEndpointsAndReturnsNoProviderToken()throws Exception {
        HttpClient client=mock(HttpClient.class);HttpResponse<String> token=response("{\"openid\":\"verified_open\",\"access_token\":\"test-access\",\"unionid\":\"verified_union\"}");
        HttpResponse<String> profile=response("{\"openid\":\"verified_open\",\"unionid\":\"verified_union\",\"nickname\":\"测试昵称\"}");
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(token).thenReturn(profile);
        WechatGateway.Verified v=gateway(client).exchange("test-code");assertEquals("verified_open",v.openId());assertEquals("verified_union",v.unionId());assertEquals("测试昵称",v.nickname());
        var captured=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client,times(2)).send(captured.capture(),any(HttpResponse.BodyHandler.class));
        for(HttpRequest r:captured.getAllValues()){assertEquals("https",r.uri().getScheme());assertEquals("api.weixin.qq.com",r.uri().getHost());}
    }
    @Test void wechatErrorDoesNotEchoSecretCodeOrUpstreamMessage()throws Exception {
        HttpClient client=mock(HttpClient.class);HttpResponse<String> rejected=response("{\"errcode\":40029,\"errmsg\":\"unsafe-debug-detail\"}");
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(rejected);
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->gateway(client).exchange("test-code"));
        assertFalse(error.getReason().contains("unsafe-debug-detail"));assertFalse(error.getReason().contains("test-code"));verify(client,times(1)).send(any(),any());
    }
    @Test void mismatchedProfileIdentityIsRejected()throws Exception {
        HttpClient client=mock(HttpClient.class);HttpResponse<String> token=response("{\"openid\":\"first\",\"access_token\":\"test-access\"}");
        HttpResponse<String> profile=response("{\"openid\":\"different\",\"nickname\":\"测试\"}");
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(token).thenReturn(profile);
        assertThrows(ResponseStatusException.class,()->gateway(client).exchange("test-code"));
    }
    @Test void missingSecretNeverCallsTheProvider(){HttpClient client=mock(HttpClient.class);TencentWechatGateway gateway=new TencentWechatGateway("wxbc866bf990e09da5","",client);
        assertFalse(gateway.available());assertThrows(ResponseStatusException.class,()->gateway.exchange("test-code"));verifyNoInteractions(client);}
}
