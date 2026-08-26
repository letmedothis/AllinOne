package com.allinone.framework.web.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.allinone.common.constant.CacheConstants;
import com.allinone.common.constant.Constants;
import com.allinone.common.core.domain.model.LoginUser;
import com.allinone.common.core.redis.RedisCache;
import com.allinone.common.utils.ServletUtils;
import com.allinone.common.utils.StringUtils;
import com.allinone.common.utils.http.UserAgentUtils;
import com.allinone.common.utils.ip.AddressUtils;
import com.allinone.common.utils.ip.IpUtils;
import com.allinone.common.utils.uuid.IdUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * token验证处理
 * 
 * @author ruoyi
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final String JIMUREPORT_PATH = "/jmreport";

    private static final String JIMUBI_PATH = "/jimubi";

    private static final String DRAG_PATH = "/drag";

    private static final String ADMIN_TOKEN_COOKIE = "Admin-Token";

    /**
     * iframe 内嵌的第三方引擎路径集合。这些路径由浏览器直接导航（无法附加自定义请求头），
     * 因此允许从 URL 参数与同源 Cookie 读取令牌，且已纳入 Security 匿名白名单，
     * 最终鉴权交由 JimuReportTokenService 内部完成。
     */
    private static final String[] EMBEDDED_ENGINE_PATHS = { JIMUREPORT_PATH, JIMUBI_PATH, DRAG_PATH };

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期（默认30分钟）
    @Value("${token.expireTime}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    /**
     * 获取用户身份信息
     * 
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                Claims claims = parseToken(token);
                // 解析对应的权限以及用户信息
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                LoginUser user = redisCache.getCacheObject(userKey);
                return user;
            }
            catch (Exception e)
            {
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户身份信息
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    /**
     * 创建令牌
     * 
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        return createToken(claims);
    }

    /**
     * 验证令牌有效期，相差不足20分钟，自动刷新缓存
     * 
     * @param loginUser 登录信息
     * @return 令牌
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     * 
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 设置用户代理信息
     * 
     * @param loginUser 登录信息
     */
    public void setUserAgent(LoginUser loginUser)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(UserAgentUtils.getBrowser(userAgent));
        loginUser.setOs(UserAgentUtils.getOperatingSystem(userAgent));
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     * @return 令牌
     */
    private String createToken(Map<String, Object> claims)
    {
        String token = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        Object username = claims.get(Constants.JWT_USERNAME);
        return username == null ? null : username.toString();
    }

    /** 根据原始 JWT 获取 Redis 中的完整登录上下文。 */
    public LoginUser getLoginUserFromToken(String token)
    {
        if (StringUtils.isEmpty(token)) return null;
        try
        {
            Claims claims = parseToken(token);
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            return StringUtils.isEmpty(uuid) ? null : redisCache.getCacheObject(getTokenKey(uuid));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    private String getToken(HttpServletRequest request)
    {
        return getToken(request, isJimuReportRequest(request));
    }

    /**
     * 为 JimuReport/JimuBI 拦截器解析令牌。浏览器直接访问或 iframe 加载报表/大屏时无法附加
     * Authorization 请求头，因此只在该集成边界允许读取 URL 参数 token 与同源 Admin-Token Cookie。
     * 其他业务接口仍只接受请求头令牌，避免把 URL/Cookie 认证扩散到整个无 CSRF 会话。
     */
    public String getJimuReportToken(HttpServletRequest request)
    {
        return getToken(request, true);
    }

    private String getToken(HttpServletRequest request, boolean allowEmbeddedToken)
    {
        String token = request.getHeader(header);
        if (StringUtils.isEmpty(token))
        {
            token = request.getHeader("X-Access-Token");
        }
        if (StringUtils.isEmpty(token) && allowEmbeddedToken)
        {
            // iframe 导航时由前端把令牌拼在 URL query 上（如 /jmreport/view/xx?token=xxx）
            token = request.getParameter("token");
        }
        if (StringUtils.isEmpty(token) && allowEmbeddedToken)
        {
            token = getCookieValue(request, ADMIN_TOKEN_COOKIE);
        }
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.substring(Constants.TOKEN_PREFIX.length());
        }
        return token;
    }

    private boolean isJimuReportRequest(HttpServletRequest request)
    {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.isNotEmpty(contextPath) && requestUri.startsWith(contextPath))
        {
            requestUri = requestUri.substring(contextPath.length());
        }
        for (String enginePath : EMBEDDED_ENGINE_PATHS)
        {
            if (enginePath.equals(requestUri) || requestUri.startsWith(enginePath + "/"))
            {
                return true;
            }
        }
        return false;
    }

    private String getCookieValue(HttpServletRequest request, String cookieName)
    {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
        {
            return null;
        }
        for (Cookie cookie : cookies)
        {
            if (cookieName.equals(cookie.getName()))
            {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

    /**
     * 角色权限变更后，刷新所有持有该角色的在线用户权限
     *
     * @param roleId            变更的角色ID
     * @param permissionService 权限服务
     */
    public void refreshPermissionByRoleId(Long roleId, SysPermissionService permissionService)
    {
        // 扫描所有在线 token
        String pattern = CacheConstants.LOGIN_TOKEN_KEY + "*";
        Collection<String> keys = redisCache.keys(pattern);
        if (keys == null || keys.isEmpty())
        {
            return;
        }
        for (String key : keys)
        {
            LoginUser loginUser = redisCache.getCacheObject(key);
            if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().isAdmin())
            {
                // 管理员拥有所有权限，跳过
                continue;
            }
            // 判断该用户是否拥有此角色
            boolean hasRole = loginUser.getUser().getRoles() != null
                    && loginUser.getUser().getRoles().stream().anyMatch(r -> roleId.equals(r.getRoleId()));
            if (!hasRole)
            {
                continue;
            }
            // 刷新权限缓存
            loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
            refreshToken(loginUser);
            log.info("角色[{}]权限变更，已刷新在线用户[{}]的权限缓存", roleId, loginUser.getUsername());
        }
    }
}
