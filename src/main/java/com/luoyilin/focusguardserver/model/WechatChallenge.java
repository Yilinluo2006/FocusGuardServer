package com.luoyilin.focusguardserver.model;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="wechat_challenges")
public class WechatChallenge {
    public enum Purpose { LOGIN, BIND }
    @Id @Column(length=64) private String tokenHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=8) private Purpose purpose;
    private Long ownerId;
    @Column(nullable=false) private Instant expiresAt;
    private boolean consumed;
    protected WechatChallenge(){}
    public WechatChallenge(String hash,Purpose purpose,Long owner,Instant expires){tokenHash=hash;this.purpose=purpose;ownerId=owner;expiresAt=expires;}
    public boolean accepts(Purpose expected,Long owner,Instant now){return !consumed&&purpose==expected&&java.util.Objects.equals(ownerId,owner)&&expiresAt.isAfter(now);}
    public void consume(){consumed=true;}
    public Instant getExpiresAt(){return expiresAt;}
}
