package com.betta.robot.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.betta.common.core.domain.entity.SysUser;
import com.betta.common.core.domain.model.LoginUser;
import com.betta.common.core.redis.RedisCache;
import com.betta.common.enums.UserStatus;
import com.betta.common.exception.ServiceException;
import com.betta.common.utils.StringUtils;
import com.betta.common.utils.uuid.IdUtils;
import com.betta.framework.web.service.TokenService;
import com.betta.framework.web.service.UserDetailsServiceImpl;
import com.betta.robot.dto.AutoLoginResult;
import com.betta.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 机器人自动登录 ticket 服务。
 *
 * <p>设计目标：
 * 1. 机器人只把一次性 ticket 发给用户，不在聊天消息里暴露账号密码或系统 token。
 * 2. 前端访问 /auto-login?ticket=xxx 后，用 ticket 向后端兑换正常登录 token。
 * 3. ticket 使用后立即删除，避免同一个链接被重复打开或转发后长期可用。
 * 4. ticket 中保存 targetPath，后续其它模块也可以复用同一套自动登录能力。
 * </p>
 *
 * @author betta
 */
@Service
public class RobotAutoLoginService {

    /** Redis 中保存自动登录 ticket 的 key 前缀。 */
    private static final String TICKET_KEY_PREFIX = "robot:auto-login:";

    /** 配置未指定过期时间时，默认 5 分钟有效。 */
    private static final int DEFAULT_EXPIRE_MINUTES = 5;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private TokenService tokenService;

    public String createTicket(String username, String targetPath, Integer expireMinutes) {
        if (StringUtils.isBlank(username)) {
            throw new ServiceException("自动登录用户不能为空");
        }
        // targetPath 只能是站内相对路径，防止把自动登录能力变成外部跳转入口。
        if (!isSafeTargetPath(targetPath)) {
            throw new ServiceException("自动登录目标页面不能为空");
        }
        int ttl = expireMinutes == null || expireMinutes <= 0 ? DEFAULT_EXPIRE_MINUTES : expireMinutes;
        String ticket = IdUtils.fastUUID();

        // ticket 的载荷只放必要信息：登录用户、登录成功后的目标页、业务层过期时间。
        // Redis 本身也设置了 TTL；这里额外保存 expireTime 是为了兑换时给出更明确的过期判断。
        JSONObject payload = new JSONObject();
        payload.put("username", username);
        payload.put("targetPath", targetPath);
        payload.put("expireTime", System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(ttl));
        redisCache.setCacheObject(getTicketKey(ticket), payload.toJSONString(), ttl, TimeUnit.MINUTES);
        return ticket;
    }

    public AutoLoginResult exchangeToken(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            throw new ServiceException("自动登录链接无效");
        }
        String ticketKey = getTicketKey(ticket);
        String payloadText = redisCache.getCacheObject(ticketKey);

        // 先删除再校验，保证 ticket 只能兑换一次。
        // 即使后续校验失败，也不会留下可重放的自动登录凭证。
        redisCache.deleteObject(ticketKey);
        if (StringUtils.isBlank(payloadText)) {
            throw new ServiceException("自动登录链接已失效");
        }

        JSONObject payload = JSON.parseObject(payloadText);
        Long expireTime = payload.getLong("expireTime");
        if (expireTime == null || expireTime < System.currentTimeMillis()) {
            throw new ServiceException("自动登录链接已过期");
        }

        String username = payload.getString("username");
        String targetPath = payload.getString("targetPath");
        if (!isSafeTargetPath(targetPath)) {
            throw new ServiceException("自动登录目标页面无效");
        }

        // 兑换 token 时重新查询用户状态，不直接信任 ticket 中的 username。
        // 这样用户被删除或停用后，旧 ticket 也无法继续登录。
        SysUser user = userService.selectUserByUserName(username);
        if (user == null || UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            throw new ServiceException("自动登录用户不存在");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            throw new ServiceException("自动登录用户已停用");
        }

        // 复用系统原有 LoginUser 和 TokenService，生成的 token 与普通登录保持一致。
        // 后续权限、路由、接口鉴权仍然走现有体系。
        LoginUser loginUser = (LoginUser) userDetailsService.createLoginUser(user);
        return new AutoLoginResult(tokenService.createToken(loginUser), targetPath);
    }

    private String getTicketKey(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }

    private boolean isSafeTargetPath(String targetPath) {
        // 只允许 /dance、/foo/bar 这类站内路径。
        // 禁止 //evil.com 和 http://evil.com，避免开放重定向风险。
        return StringUtils.isNotBlank(targetPath)
                && targetPath.startsWith("/")
                && !targetPath.startsWith("//")
                && !targetPath.contains("://");
    }
}
