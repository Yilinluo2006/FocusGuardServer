//Controller 只处理 HTTP 请求，Service 负责数据和业务逻辑
package com.luoyilin.focusguardserver.service;

import com.luoyilin.focusguardserver.model.AppLimit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppLimitService {

    private final List<AppLimit> appLimits = new ArrayList<>();
    //在service中保存应用限制数据

    public AppLimitService() {

        //service创建时加入两条测试数
        appLimits.add(new AppLimit(
                "com.instagram.android",
                30,
                true
));
        appLimits.add(new AppLimit(
                "tv.danmaku.bili",
                45,
                true
        ));
    }
    public List<AppLimit> getAllLimits() {

        // 返回全部应用限制
        return appLimits;
    }

    public AppLimit addLimit(AppLimit appLimit) {

        // 将新的应用限制添加到列表
        appLimits.add(appLimit);

        // 返回刚刚添加的数据
        return appLimit;
    }
}