package com.makefriends.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.makefriends.dto.LoginDTO;
import com.makefriends.dto.RegisterDTO;
import com.makefriends.dto.UserUpdateDTO;
import com.makefriends.vo.LoginVO;
import com.makefriends.vo.UserVO;

public interface UserService {

    UserVO register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    void logout();

    UserVO getMyInfo();

    UserVO updateMyInfo(UserUpdateDTO dto);

    UserVO getUserById(Long id);

    IPage<UserVO> getUserList(int page, int size, Integer gender, String city, Integer minAge, Integer maxAge, String occupation, String hobby);

    /**
     * 心跳：标记活跃时间
     */
    void heartbeat();

    /**
     * 设置在线状态：1=在线，2=隐身
     */
    void setOnlineStatus(Integer status);

    /**
     * 统一判定某用户是否"对外显示在线"：onlineStatus=1 且 lastActiveAt 在 60 秒内
     */
    boolean isUserOnline(Long userId);
}
