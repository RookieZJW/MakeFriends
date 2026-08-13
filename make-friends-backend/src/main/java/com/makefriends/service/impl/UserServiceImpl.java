package com.makefriends.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.makefriends.dto.LoginDTO;
import com.makefriends.dto.RegisterDTO;
import com.makefriends.dto.UserUpdateDTO;
import com.makefriends.entity.User;
import com.makefriends.mapper.UserMapper;
import com.makefriends.service.UserService;
import com.makefriends.util.PasswordUtil;
import com.makefriends.vo.LoginVO;
import com.makefriends.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Value("${file.upload.path}")
    private String uploadPath;

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (count > 0) {
            throw new IllegalArgumentException("该手机号已注册");
        }
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setGender(dto.getGender());
        user.setStatus(1);
        userMapper.insert(user);
        return toVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        // 登录即标记为在线，并刷新活跃时间
        user.setOnlineStatus(1);
        user.setLastActiveAt(LocalDateTime.now());
        userMapper.updateById(user);

        StpUtil.login(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUserInfo(toSelfVO(user));
        return vo;
    }

    @Override
    public void logout() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            User user = userMapper.selectById(userId);
            if (user != null) {
                // 登出后对外显示离线（不管之前是不是隐身）
                if (user.getOnlineStatus() != null && user.getOnlineStatus() == 1) {
                    user.setOnlineStatus(0);
                }
                userMapper.updateById(user);
            }
        } catch (Exception ignore) {
        }
        StpUtil.logout();
    }

    @Override
    public UserVO getMyInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 打开自己信息页也算一次活跃，保证自身展示为在线
        if (user.getOnlineStatus() == null || user.getOnlineStatus() != 2) {
            user.setOnlineStatus(1);
        }
        user.setLastActiveAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toSelfVO(user);
    }

    @Override
    public UserVO updateMyInfo(UserUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (dto.getPhone() != null) {
            throw new IllegalArgumentException("禁止修改手机号");
        }
        if (dto.getPassword() != null) {
            throw new IllegalArgumentException("禁止通过此接口修改密码");
        }
        if (dto.getStatus() != null) {
            throw new IllegalArgumentException("禁止修改账号状态");
        }
        if (dto.getNickname() != null && dto.getNickname().length() > 30) {
            throw new IllegalArgumentException("昵称长度不能超过30个字符");
        }
        if (dto.getSignature() != null && dto.getSignature().length() > 200) {
            throw new IllegalArgumentException("个性签名长度不能超过200个字符");
        }
        if (dto.getAge() != null && (dto.getAge() < 14 || dto.getAge() > 120)) {
            throw new IllegalArgumentException("年龄必须在14-120之间");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String oldAvatar = user.getAvatar();
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null && !dto.getAvatar().equals(oldAvatar)) {
            user.setAvatar(dto.getAvatar());
            safeDeleteAvatarFile(oldAvatar);
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getAge() != null) {
            user.setAge(dto.getAge());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getBirthday() != null) {
            user.setBirthday(dto.getBirthday());
        }
        if (dto.getHeight() != null) {
            user.setHeight(dto.getHeight());
        }
        if (dto.getWeight() != null) {
            user.setWeight(dto.getWeight());
        }
        if (dto.getCity() != null) {
            user.setCity(dto.getCity());
        }
        if (dto.getOccupation() != null) {
            user.setOccupation(dto.getOccupation());
        }
        if (dto.getSignature() != null) {
            user.setSignature(dto.getSignature());
        }
        if (dto.getHobbies() != null) {
            user.setHobbies(dto.getHobbies());
        }
        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public IPage<UserVO> getUserList(int page, int size, Integer gender, String city, Integer minAge, Integer maxAge, String occupation, String hobby) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);
        if (gender != null) {
            wrapper.eq(User::getGender, gender);
        }
        if (city != null && !city.isEmpty()) {
            wrapper.eq(User::getCity, city);
        }
        if (minAge != null) {
            wrapper.ge(User::getAge, minAge);
        }
        if (maxAge != null) {
            wrapper.le(User::getAge, maxAge);
        }
        if (occupation != null && !occupation.isEmpty()) {
            wrapper.eq(User::getOccupation, occupation);
        }
        if (hobby != null && !hobby.isEmpty()) {
            wrapper.like(User::getHobbies, hobby);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        Page<User> pageObj = new Page<>(page, size);
        IPage<User> userPage = userMapper.selectPage(pageObj, wrapper);
        return userPage.convert(this::toVO);
    }

    @Override
    public void heartbeat() {
        Long myId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(myId);
        if (user == null) return;
        // 只有当状态不是隐身时，心跳才保持"在线"
        if (user.getOnlineStatus() == null || user.getOnlineStatus() == 0) {
            user.setOnlineStatus(1);
        }
        user.setLastActiveAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void setOnlineStatus(Integer status) {
        if (status == null || (status != 1 && status != 2)) {
            throw new IllegalArgumentException("状态非法");
        }
        Long myId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(myId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        user.setOnlineStatus(status);
        user.setLastActiveAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public boolean isUserOnline(Long userId) {
        if (userId == null) return false;
        User u = userMapper.selectById(userId);
        if (u == null) return false;
        // 隐身：对外离线
        if (u.getOnlineStatus() != null && u.getOnlineStatus() == 2) return false;
        // onlineStatus = 1 且 lastActiveAt 在 60 秒内
        if (u.getOnlineStatus() != null && u.getOnlineStatus() == 1 && u.getLastActiveAt() != null) {
            return Duration.between(u.getLastActiveAt(), LocalDateTime.now()).getSeconds() <= 60;
        }
        return false;
    }

    private void safeDeleteAvatarFile(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) return;
        if (avatarUrl.startsWith("http")) return;
        String fileName = null;
        if (avatarUrl.startsWith("/files/")) {
            fileName = avatarUrl.substring("/files/".length());
        } else if (avatarUrl.startsWith("/upload/")) {
            fileName = avatarUrl.substring("/upload/".length());
        } else if (avatarUrl.startsWith("/")) {
            fileName = avatarUrl.substring(1);
        } else {
            fileName = avatarUrl;
        }
        if (fileName == null || fileName.isEmpty()) return;
        try {
            File file = new File(uploadPath, fileName);
            if (file.exists() && file.isFile()) {
                boolean ignored = file.delete();
            }
        } catch (Exception e) {
        }
    }

    private UserVO toVO(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setOnline(isUserOnline(user.getId()));
        // 查别人信息时，不泄露自己的 onlineStatus 详细值
        vo.setOnlineStatus(null);
        return vo;
    }

    private UserVO toSelfVO(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 给自己看：online 由 onlineStatus 计算
        int s = user.getOnlineStatus() == null ? 0 : user.getOnlineStatus();
        vo.setOnline(s == 1 || s == 2);
        if (s == 1) vo.setOnlineStatus(1);
        else if (s == 2) vo.setOnlineStatus(2);
        else vo.setOnlineStatus(0);
        return vo;
    }
}