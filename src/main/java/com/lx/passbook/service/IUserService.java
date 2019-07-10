package com.lx.passbook.service;

import com.lx.passbook.vo.Response;
import com.lx.passbook.vo.User;

/**
 * 用户服务：创建User服务
 */
public interface IUserService {

    /**
     * <h2>创建用户</h2>
     * @param user {@link User}
     * @return {@link Response}
     * */
    Response createUser(User user) throws Exception;
}
