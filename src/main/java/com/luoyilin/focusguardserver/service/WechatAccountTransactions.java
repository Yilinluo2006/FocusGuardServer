package com.luoyilin.focusguardserver.service;
import com.luoyilin.focusguardserver.model.*;
import com.luoyilin.focusguardserver.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service public class WechatAccountTransactions {
    private final UserRepository users;private final AccountIdentityRepository identities;private final AccountService accounts;private final WechatGateway gateway;
    public WechatAccountTransactions(UserRepository users,AccountIdentityRepository identities,AccountService accounts,WechatGateway gateway){this.users=users;this.identities=identities;this.accounts=accounts;this.gateway=gateway;}
    private User owner(WechatGateway.Verified verified){
        User open=identities.findByProviderAndIssuerAndSubject(AccountIdentity.Provider.WECHAT,gateway.appId(),verified.openId()).map(AccountIdentity::getUser).orElse(null);
        User union=verified.unionId().isEmpty()?null:identities.findByProviderAndIssuerAndSubject(AccountIdentity.Provider.WECHAT,"wechat-union",verified.unionId()).map(AccountIdentity::getUser).orElse(null);
        if(open!=null&&union!=null&&!open.getId().equals(union.getId()))throw conflict();return open==null?union:open;
    }
    private void attach(User user,WechatGateway.Verified verified){
        for(String[] pair:new String[][]{{gateway.appId(),verified.openId()},{"wechat-union",verified.unionId()}}){
            if(pair[1].isEmpty())continue;
            AccountIdentity current=identities.findByUserAndProviderAndIssuer(user,AccountIdentity.Provider.WECHAT,pair[0]).orElse(null);
            if(current!=null&&!current.getSubject().equals(pair[1]))throw new ResponseStatusException(HttpStatus.CONFLICT,"当前账号已绑定其他微信，暂不支持直接更换");
            accounts.bindVerifiedIdentity(user,AccountIdentity.Provider.WECHAT,pair[0],pair[1]);
        }
        user.verifyWechat();users.saveAndFlush(user);
    }
    @Transactional public User login(WechatGateway.Verified verified){
        User found=owner(verified);User user;
        if(found!=null)user=users.lockById(found.getId()).orElseThrow(()->conflict());
        else {user=User.createDefault(null,null);String nickname=verified.nickname()==null?"":verified.nickname().strip();
            StringBuilder clean=new StringBuilder();nickname.codePoints().filter(c->!Character.isISOControl(c)).limit(24).forEach(clean::appendCodePoint);
            if(!clean.toString().isBlank())user.updateProfile(clean.toString(),"default");user.verifyWechat();user=users.saveAndFlush(user);}
        attach(user,verified);return user;
    }
    @Transactional public User bind(Long principal,WechatGateway.Verified verified){
        User user=users.lockById(principal).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请重新登录"));
        User occupied=owner(verified);if(occupied!=null&&!occupied.getId().equals(user.getId()))throw conflict();attach(user,verified);return user;
    }
    private ResponseStatusException conflict(){return new ResponseStatusException(HttpStatus.CONFLICT,"该微信已属于其他账号，请登录原账号，不会自动合并数据");}
}
