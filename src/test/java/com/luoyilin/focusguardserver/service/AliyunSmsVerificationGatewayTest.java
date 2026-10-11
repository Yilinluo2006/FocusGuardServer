package com.luoyilin.focusguardserver.service;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.*;
import com.aliyun.tea.TeaException;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AliyunSmsVerificationGatewayTest {
    private AliyunSmsVerificationGateway gateway;
    private Client client;
    @BeforeEach void setup() throws Exception {gateway=new AliyunSmsVerificationGateway("","","fixture");client=mock(Client.class);ReflectionTestUtils.setField(gateway,"client",client);}
    @Test void providerExceptionForWrongCodeIsAuthenticationFailure() throws Exception {
        when(client.checkSmsVerifyCodeWithOptions(any(),any())).thenThrow(new TeaException(Map.of("code","isv.ValidateFail","statusCode",400,"message","private provider message")));
        assertFalse(gateway.verify("13800000000","fixture","123456"));
    }
    @Test void validationFailureInResponseIsAlsoNotServiceOutage() throws Exception {
        when(client.checkSmsVerifyCodeWithOptions(any(),any())).thenReturn(new CheckSmsVerifyCodeResponse().setBody(new CheckSmsVerifyCodeResponseBody().setCode("isv.ValidateFail").setSuccess(false)));
        assertFalse(gateway.verify("13800000000","fixture","123456"));
    }
    @Test void providerFrequencyExceptionRemainsRateLimit() throws Exception {
        when(client.sendSmsVerifyCodeWithOptions(any(),any())).thenThrow(new TeaException(Map.of("code","biz.FREQUENCY","statusCode",400)));
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->gateway.send("13800000000","fixture","100001"));assertEquals(429,error.getStatusCode().value());
    }
    @Test void permissionFailureRetainsSafeCodeAndHidesProviderMessage() throws Exception {
        when(client.sendSmsVerifyCodeWithOptions(any(),any())).thenThrow(new TeaException(Map.of("code","isv.RAM_PERMISSION_DENY","statusCode",403,"message","private-key-in-provider-message")));
        ResponseStatusException error=assertThrows(ResponseStatusException.class,()->gateway.send("13800000000","fixture","100001"));assertEquals(503,error.getStatusCode().value());assertTrue(error.getReason().contains("isv.RAM_PERMISSION_DENY"));assertFalse(error.getReason().contains("private-key"));
    }
    @Test void suspendedServiceIsReportedClearlyForResponseAndSdkException() throws Exception {
        when(client.sendSmsVerifyCodeWithOptions(any(),any())).thenReturn(new SendSmsVerifyCodeResponse().setBody(new SendSmsVerifyCodeResponseBody().setSuccess(false).setCode("isv.OUT_OF_SERVICE")));
        ResponseStatusException response=assertThrows(ResponseStatusException.class,()->gateway.send("13800000000","fixture","100001"));
        assertEquals(503,response.getStatusCode().value());assertTrue(response.getReason().contains("余额不足或已停用"));assertTrue(response.getReason().contains("isv.OUT_OF_SERVICE"));
        when(client.sendSmsVerifyCodeWithOptions(any(),any())).thenThrow(new TeaException(Map.of("code","isv.OUT_OF_SERVICE","statusCode",400,"message","private-provider-details")));
        ResponseStatusException exception=assertThrows(ResponseStatusException.class,()->gateway.send("13800000000","fixture","100001"));
        assertTrue(exception.getReason().contains("isv.OUT_OF_SERVICE"));assertFalse(exception.getReason().contains("private-provider-details"));
    }
    @Test void transportSuccessWithoutPassCannotAuthenticate() throws Exception {
        var body=new CheckSmsVerifyCodeResponseBody().setCode("OK").setSuccess(true).setModel(new CheckSmsVerifyCodeResponseBody.CheckSmsVerifyCodeResponseBodyModel().setVerifyResult("UNKNOWN"));
        when(client.checkSmsVerifyCodeWithOptions(any(),any())).thenReturn(new CheckSmsVerifyCodeResponse().setBody(body));assertFalse(gateway.verify("13800000000","fixture","123456"));
    }
}
