package com.luoyilin.focusguardserver.service;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.aliyun.tea.TeaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AliyunSmsVerificationGateway implements SmsVerificationGateway {
    private final Client client;
    private final String signature;
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(AliyunSmsVerificationGateway.class);
    public AliyunSmsVerificationGateway(@Value("${ALIBABA_CLOUD_ACCESS_KEY_ID:}") String id,
            @Value("${ALIBABA_CLOUD_ACCESS_KEY_SECRET:}") String secret,
            @Value("${ALIYUN_SMS_SIGN_NAME:恒创联众}") String signature) throws Exception {
        this.signature = signature;
        client = id.isBlank() || secret.isBlank() ? null : new Client(new Config().setAccessKeyId(id).setAccessKeySecret(secret)
                .setEndpoint("dypnsapi.aliyuncs.com"));
        if(client==null) log.warn("PNVS SMS credentials configured: false; check local .env or server environment");
        else log.info("PNVS SMS credentials configured: true");
    }
    private RuntimeOptions runtime() { return new RuntimeOptions().setConnectTimeout(5000).setReadTimeout(5000).setAutoretry(false); }
    @Override public void send(String phone, String scheme, String template) {
        if (client == null) throw missingConfiguration();
        try {
            var response = client.sendSmsVerifyCodeWithOptions(new SendSmsVerifyCodeRequest().setPhoneNumber(phone)
                    .setSchemeName(scheme).setCountryCode("86").setSignName(signature).setTemplateCode(template)
                    .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}").setCodeLength(6L).setCodeType(1L)
                    .setValidTime(300L).setInterval(60L).setDuplicatePolicy(1L).setReturnVerifyCode(false), runtime()).getBody();
            if (!Boolean.TRUE.equals(response.getSuccess()) || !"OK".equals(response.getCode())) {
                throw providerFailure("send",response.getCode());
            }
        } catch (ResponseStatusException error) { throw error; }
        catch (TeaException error) { throw providerFailure("send",error.getCode()); }
        catch (Exception error) { log.warn("PNVS send transport failure: {}", error.getClass().getSimpleName()); throw unavailable(); }
    }
    @Override public boolean verify(String phone, String scheme, String code) {
        if (client == null) throw missingConfiguration();
        try {
            var response = client.checkSmsVerifyCodeWithOptions(new CheckSmsVerifyCodeRequest().setPhoneNumber(phone)
                    .setSchemeName(scheme).setCountryCode("86").setVerifyCode(code), runtime()).getBody();
            if (!Boolean.TRUE.equals(response.getSuccess()) || !"OK".equals(response.getCode())) {
                if (validationFailed(response.getCode())) return false;
                throw providerFailure("verify",response.getCode());
            }
            // Code=OK/Success=true mean transport success only. PASS is the verified identity proof.
            return response.getModel() != null && "PASS".equals(response.getModel().getVerifyResult());
        } catch (ResponseStatusException error) { throw error; }
        catch (TeaException error) {
            if(validationFailed(error.getCode())) return false;
            throw providerFailure("verify",error.getCode());
        }
        catch (Exception error) { log.warn("PNVS verify transport failure: {}",error.getClass().getSimpleName()); throw unavailable(); }
    }
    private static boolean validationFailed(String code) { return "isv.ValidateFail".equals(code) || "ValidateFail".equals(code); }
    private ResponseStatusException missingConfiguration() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"后端未配置短信密钥，请重新启动后端并检查配置");
    }
    private ResponseStatusException providerFailure(String operation,String code) {
        String safe=code!=null&&code.matches("[A-Za-z0-9_.-]{1,100}")?code:"UNKNOWN";
        // Only the machine-readable code is logged. SDK messages/data may contain credentials or phone numbers.
        log.warn("PNVS {} rejected: code={}",operation,safe);
        if(java.util.Set.of("FREQUENCY_FAIL","BUSINESS_LIMIT_CONTROL","biz.FREQUENCY","isv.BUSINESS_LIMIT_CONTROL","isp.QPS_LIMIT").contains(safe))
            return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"短信发送过于频繁，请稍后重试");
        if(safe.contains("PERMISSION_DENY") || safe.startsWith("Forbidden") || safe.startsWith("AccessDenied"))
            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"短信服务权限不足，请检查阿里云 RAM 授权（"+safe+"）");
        if(java.util.Set.of("isv.OUT_OF_SERVICE","isv.AMOUNT_NOT_ENOUGH","InsufficientBalance").contains(safe))
            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"短信服务余额不足或已停用（"+safe+"）");
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"短信认证暂不可用（"+safe+"），请稍后重试或使用邮箱登录");
    }
    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "短信认证暂不可用，请稍后重试或使用邮箱登录");
    }
}
