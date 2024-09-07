本示例参考了网上资料：

* https://blog.csdn.net/qq_35144624/article/details/130928116


```
认证流程：
1、前端请求 http://localhost:8443/cas/login?service=http%3A%2F%2Flocalhost%3A8034%2Fapi%2Fadmin%2Fauth 跳转单点登录  
2、登录成功回调 http://localhost:8081/admin/auth 接口，即 service 指定的接口地址，携带 ticket 票据及用户信息，重定向到应用系统首页（http://localhost:8081）      

退出登录：https://localhost:8443/cas/loginOption?originalUrl=http%3A%2F%2Flocalhost%3A8034%2Fapi%2Fadmin%2Fauth  
```


### 1、引入依赖

```xml
<dependencies>
    <!-- 支持 servlet -->
    <dependency>
        <groupId>org.noear</groupId>
        <artifactId>solon-boot-jetty</artifactId>
    </dependency>

    <dependency>
        <groupId>org.jasig.cas.client</groupId>
        <artifactId>cas-client-core</artifactId>
        <version>3.6.4</version>
    </dependency>
</dependencies>
```

### 2、添加应用配置

```yaml
server:
  port: 8081
  contextPath: /api #可选

cas:
  #CAS服务端地址
  cas-server-url-prefix: https://localhost:8443/cas
  #CAS服务端登录地址
  cas-server-login-Url: ${cas.cas-server-url-prefix}/login
  #应用系统地址
  server-name: http://localhost:8081
  #受保护的url前缀
  authentication-url-patterns: /admin/*
  #认证成功重定向地址
  redirect-url-success: ${cas.server-name}/#/Index
  #注销登录重定向地址，service=http://localhost:8034/api/admin/auth 是API服务认证接口的地址
  redirect-url-logout: ${cas.cas-server-url-prefix}/logout?service=http%3A%2F%2Flocalhost%3A8034%2Fapi%2Fadmin%2Fauth

```

### 3、构建 Jasig-CAS 初始化配置器

```java

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
```

### 4、添加登录与退出集成代码


```java

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

            ctx.sessionState().sessionReset();
            ctx.headerSet("Content-type", "text/html;charset=UTF-8");
            ctx.redirect(logoutRedirectUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```