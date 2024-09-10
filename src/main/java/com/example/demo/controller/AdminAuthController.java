package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.jasig.cas.client.validation.Assertion;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.core.handle.Context;

import java.util.Collection;

@Controller
@Mapping("/admin/auth")
@Slf4j
public class AdminAuthController {
    @Inject("${cas.redirect-url-success}")
    private String successRedirectUrl;
    @Inject("${cas.redirect-url-logout}")
    private String logoutRedirectUrl;


    /**
     * 登录认证
     */
    @Mapping("/auth")
    public void auth(String redirectUrl, Context ctx) throws Exception {
        log.info("登录认证");
        Assertion assertion = ctx.session("_const_cas_assertion_", Assertion.class);

        String userId = assertion.getPrincipal().getName();
        log.info("sessionId：{}", ctx.sessionId());
        log.info("userId：{}", userId);

        Collection<String> headerNames = ctx.headerNames();
        log.info("headerNames：{}", headerNames);

        redirectUrl = null != redirectUrl && !redirectUrl.equals("") ?
                redirectUrl : successRedirectUrl;
        log.info("redirectUrl：{}", redirectUrl);
    }

    /**
     * 退出登录
     */
    @Mapping("/logout")
    public void logout(Context ctx) throws Exception {
        try {
            log.info("退出登录");
            log.info("sessionId：{}", ctx.sessionId());
            log.info("logoutRedirectUrl：{}", logoutRedirectUrl);

            ctx.sessionReset();
            ctx.headerSet("Content-type", "text/html;charset=UTF-8");
            ctx.redirect(logoutRedirectUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}