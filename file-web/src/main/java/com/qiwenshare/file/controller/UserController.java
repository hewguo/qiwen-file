package com.qiwenshare.file.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.qiwenshare.common.result.RestResult;
import com.qiwenshare.common.domain.AliyunOSS;
import com.qiwenshare.common.util.JjwtUtil;
import com.qiwenshare.file.anno.MyLog;
import com.qiwenshare.file.api.IUserService;
import com.qiwenshare.common.config.QiwenFileConfig;
import com.qiwenshare.file.domain.UserBean;
import com.qiwenshare.file.dto.user.RegisterDTO;
import com.qiwenshare.file.vo.user.UserLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制类
 *
 * @author ma116
 */
@Tag(name = "user", description = "该接口为用户接口，主要做用户登录，注册和校验token")
@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Resource
    IUserService userService;

    @Autowired
    QiwenFileConfig qiwenFileConfig;


    public static Map<String, String> verificationCodeMap = new HashMap<>();

    public static final int TEXT = 4;

    /**
     * 当前模块
     */
    public static final String CURRENT_MODULE = "用户管理";


    @Operation(summary = "用户注册", description = "注册账号", tags = {"user"})
    @PostMapping(value = "/register")
    @MyLog(operation = "用户注册", module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<String> addUser(@RequestBody RegisterDTO registerDTO) {
        RestResult<String> restResult = null;
        UserBean userBean = new UserBean();
        BeanUtil.copyProperties(registerDTO, userBean);
        restResult = userService.registerUser(userBean);

        return restResult;
    }

    @Operation(summary = "用户登录", description = "用户登录认证后才能进入系统", tags = {"user"})
    @GetMapping("/login")
    @MyLog(operation = "用户登录", module = CURRENT_MODULE)
    @ResponseBody
    public RestResult<UserLoginVo> userLogin(
            @Parameter(description = "登录手机号") String username,
            @Parameter(description = "登录密码") String password) {
        RestResult<UserLoginVo> restResult = new RestResult<UserLoginVo>();
        UserBean saveUserBean = userService.findUserInfoByTelephone(username);

        if (saveUserBean == null) {
            return RestResult.fail().message("用户名或手机号不存在！");
        }
        String jwt = "";
        try {
            jwt = JjwtUtil.createJWT("qiwenshare", "qiwen", JSON.toJSONString(saveUserBean));
        } catch (Exception e) {
            log.info("登录失败：{}", e);
            restResult.setSuccess(false);
            restResult.setMessage("登录失败！");
            return restResult;
        }

        String passwordHash = new SimpleHash("MD5", password, saveUserBean.getSalt(), 1024).toHex();
        if (passwordHash.equals(saveUserBean.getPassword())) {

            UserLoginVo userLoginVo = new UserLoginVo();
            BeanUtil.copyProperties(saveUserBean, userLoginVo);
            userLoginVo.setToken(jwt);
            restResult.setData(userLoginVo);
            restResult.setSuccess(true);
        } else {
            restResult.setSuccess(false);
            restResult.setMessage("手机号或密码错误！");
        }

        return restResult;
    }

    @Operation(summary = "检查用户登录信息", description = "验证token的有效性", tags = {"user"})
    @GetMapping("/checkuserlogininfo")
    @ResponseBody
    public RestResult<UserBean> checkUserLoginInfo(@RequestHeader("token") String token) {

        if ("undefined".equals(token) || StringUtils.isEmpty(token)) {
            return RestResult.fail().message("用户暂未登录");
        }
        UserBean sessionUserBean = userService.getUserBeanByToken(token);
        if (sessionUserBean != null) {

            AliyunOSS oss = qiwenFileConfig.getAliyun().getOss();
            String domain = oss.getDomain();
            sessionUserBean.setViewDomain(domain);
            return RestResult.success().data(sessionUserBean);

        } else {
            return RestResult.fail().message("用户暂未登录");
        }

    }

}
