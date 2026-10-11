package com.luoyilin.focusguardserver.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** AppSecret and provider tokens stay here; callers receive only server-verified identity fields. */
@Service public class TencentWechatGateway implements WechatGateway {
    private final String appId,secret;
    private final HttpClient client;
    private final ObjectMapper json=new ObjectMapper();
    @org.springframework.beans.factory.annotation.Autowired
    public TencentWechatGateway(@Value("${app.wechat.app-id}")String appId,@Value("${app.wechat.app-secret}")String secret){this(appId,secret,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());}
    TencentWechatGateway(String appId,String secret,HttpClient client){this.appId=appId;this.secret=secret;this.client=client;}
    public String appId(){return appId;}
    public boolean available(){return appId.matches("wx[0-9a-fA-F]{16}")&&!secret.isBlank();}
    private String escape(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private Map<?,?> get(String path)throws Exception {
        HttpRequest request=HttpRequest.newBuilder(URI.create("https://api.weixin.qq.com"+path)).timeout(Duration.ofSeconds(8)).GET().build();
        HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(response.statusCode()!=200||response.body().length()>65536)throw new IllegalStateException();
        return json.readValue(response.body(),Map.class);
    }
    private String value(Map<?,?> data,String key){Object value=data.get(key);return value instanceof String?(String)value:"";}
    private void check(Map<?,?> data){Object error=data.get("errcode");if(error instanceof Number&&((Number)error).intValue()!=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"微信授权无效或已过期，请重新授权");}
    public Verified exchange(String code){
        if(!available())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"微信登录暂不可用，请使用其他登录方式");
        try {
            Map<?,?> token=get("/sns/oauth2/access_token?appid="+escape(appId)+"&secret="+escape(secret)+"&code="+escape(code)+"&grant_type=authorization_code");check(token);
            String open=value(token,"openid"),access=value(token,"access_token");
            if(!open.matches("[A-Za-z0-9_-]{1,128}")||access.isEmpty())throw new IllegalStateException();
            Map<?,?> user=get("/sns/userinfo?access_token="+escape(access)+"&openid="+escape(open)+"&lang=zh_CN");check(user);
            if(!open.equals(value(user,"openid")))throw new IllegalStateException();
            String union=value(token,"unionid"),userinfoUnion=value(user,"unionid");
            if(!union.isEmpty()&&!userinfoUnion.isEmpty()&&!union.equals(userinfoUnion))throw new IllegalStateException();
            if(union.isEmpty())union=userinfoUnion;
            if(!union.isEmpty()&&!union.matches("[A-Za-z0-9_-]{1,128}"))throw new IllegalStateException();
            return new Verified(open,union,value(user,"nickname"));
        }catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}
        catch(Exception e){throw unavailable();} // Never include provider URLs, codes, tokens or secrets in logs/errors.
    }
    private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"暂时无法完成微信授权，请稍后重新尝试");}
}
