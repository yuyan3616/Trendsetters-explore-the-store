package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    /**
     * 发送验证码
     * @param phone
     * @param session
     * @return
     */

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (!RegexUtils.isCodeInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        // 2.生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 3.保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // session.setAttribute("code", code);
        // 4.发送验证码
        log.info("验证码已发送：{}", code);
        // 5.返回结果
        return Result.ok();
    }

    /**
     * 登入功能
     * @param loginForm
     * @param session
     * @return
     */
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        if (!RegexUtils.isCodeInvalid(loginForm.getPhone())) {
            return Result.fail("手机号格式错误");
        }
        // 2.从redis获取验证码并且校验验证码
        //String cachCode = (String)session.getAttribute("code");
        String cachCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());
        if (cachCode == null || !cachCode.equals(loginForm.getCode())) {
            return Result.fail("验证码错误");
        }
        // 3.根据手机号查询用户
        User user = query().eq("phone", loginForm.getPhone()).one();
        // 4.判断用户是否存在
        if (user == null) {
            // 5.不存在，创建新用户并保存
            user = createUserWithPhone(loginForm.getPhone());
        }
        // 6.将用户信息保存到redis中
        //6.1 随机生成token,作为登入令牌
        String token = UUID.randomUUID().toString(true);
        //6.2 将User对象转换为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString())); //解决Long类型数据精度丢失问题
        //6.3 将UserDTO对象存储到redis中
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
        //6.4 设置token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        // 7.返回结果
        return Result.ok(token);
    }

    @Override
    public Result logout() {
        // 获取当前登录用户
        UserDTO user = UserHolder.getUser();
        // 移除用户
        stringRedisTemplate.delete(LOGIN_USER_KEY+user.getId());
        return Result.ok();

    }

    private User createUserWithPhone(String phone) {
        User user = new User(); // 创建一个新的User对象
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 保存用户到数据库
        save(user);
        return user;
    }
}
