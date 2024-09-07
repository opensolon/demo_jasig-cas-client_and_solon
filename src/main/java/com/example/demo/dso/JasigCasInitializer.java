package com.example.demo.dso;

import org.jasig.cas.client.util.AssertionThreadLocalFilter;
import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import java.util.Set;

@Configuration
public class JasigCasInitializer implements ServletContainerInitializer {

    @Inject("${cas.cas-server-url-prefix}")
    private String casServerUrlPrefix;
    @Inject("${cas.cas-server-login-url}")
    private String casServerLoginUrl;
    @Inject("${cas.server-name}")
    private String serverName;
    @Inject("${cas.authentication-url-patterns}")
    private String authenticationUrlPatterns;

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext) throws ServletException {

        servletContext.addListener(org.jasig.cas.client.session.SingleSignOutHttpSessionListener.class);

        /**
         * 该过滤器负责对Ticket的校验工作，必须启用它
         */
        FilterRegistration.Dynamic cas20ProxyReceivingTicketValidationFilter =
                servletContext.addFilter("cas20ProxyReceivingTicketValidationFilter", Cas20ProxyReceivingTicketValidationFilter.class);
        // 这里的server是CAS服务端
        cas20ProxyReceivingTicketValidationFilter.setInitParameter("casServerUrlPrefix", casServerUrlPrefix);
        // 这里的server是应用系统地址
        cas20ProxyReceivingTicketValidationFilter.setInitParameter("serverName", serverName);
        cas20ProxyReceivingTicketValidationFilter.addMappingForUrlPatterns(null, false, authenticationUrlPatterns);

        /**
         * 该过滤器负责用户的认证工作，必须启用它
         */
        FilterRegistration.Dynamic authenticationFilter =
                servletContext.addFilter("authenticationFilter", org.jasig.cas.client.authentication.AuthenticationFilter.class);
        authenticationFilter.setInitParameter("casServerLoginUrl", casServerLoginUrl);
        authenticationFilter.setInitParameter("serverName", serverName);
        authenticationFilter.addMappingForUrlPatterns(null, false, authenticationUrlPatterns);

        /**
         * 该过滤器使得开发者可以通过org.jasig.cas.client.util.AssertionHolder来获取用户的登录名，
         * 比如AssertionHolder.getAssertion().getPrincipal().getName()
         */
        FilterRegistration.Dynamic assertionThreadLocalFilter = servletContext.addFilter("assertionThreadLocalFilter",
                AssertionThreadLocalFilter.class);
        assertionThreadLocalFilter.addMappingForUrlPatterns(null, false, authenticationUrlPatterns);

        /**
         * 该过滤器负责实现HttpServletRequest请求的包裹，比如允许开发者通过HttpServletRequest的getRemoteUser()方法获得SSO登录用户的登录名，
         * 可选配置。
         */
        FilterRegistration.Dynamic httpServletRequestWrapperFilter = servletContext.addFilter("httpServletRequestWrapperFilter",
                org.jasig.cas.client.util.HttpServletRequestWrapperFilter.class);
        httpServletRequestWrapperFilter.addMappingForUrlPatterns(null, false, authenticationUrlPatterns);

        /**
         * 该过滤器用于实现单点登出功能，必须配置
         */
        FilterRegistration.Dynamic singleSignOutFilter = servletContext.addFilter("singleSignOutFilter",
                org.jasig.cas.client.session.SingleSignOutFilter.class);
        singleSignOutFilter.addMappingForUrlPatterns(null, false, authenticationUrlPatterns);
    }

}