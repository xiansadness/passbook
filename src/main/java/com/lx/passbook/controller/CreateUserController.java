package com.lx.passbook.controller;

import com.lx.passbook.log.LogConstants;
import com.lx.passbook.log.LogGenerator;
import com.lx.passbook.service.IUserService;
import com.lx.passbook.vo.Response;
import com.lx.passbook.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 创建用户服务
 */

@Slf4j
@RestController
@RequestMapping("/passbook")
public class CreateUserController {

    /** 创建用户服务 */
    private final IUserService userService;

    /** HttpServletRequest */
    private final HttpServletRequest httpServletRequest;

    @Autowired
    public CreateUserController(IUserService userService,
                                HttpServletRequest httpServletRequest) {
        this.userService = userService;
        this.httpServletRequest = httpServletRequest;
    }

    /**
     * <h2>创建用户</h2>
     * @param user {@link User}
     * @return {@link Response}
     * */
    @ResponseBody
    @PostMapping("/createuser")
    Response createUser(@RequestBody User user) throws Exception {

        LogGenerator.genLog(
                httpServletRequest,
                -1L,
                LogConstants.ActionName.CREATE_USER,
                user
        );
        return userService.createUser(user);
    }
}
