package com.makefriends.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.makefriends.vo.UserVO;
import java.util.Map;

public interface UserMatchService {

    boolean likeUser(Long toUserId);

    boolean unlikeUser(Long toUserId);

    Integer getMatchStatus(Long userId);

    IPage<UserVO> getMyLikes(int page, int size);

    IPage<UserVO> getWhoLikesMe(int page, int size);

    IPage<UserVO> getMutualMatches(int page, int size);

    /** 一次性返回三类计数：match(互相匹配) / myLike(我喜欢的) / likedMe(喜欢我的) */
    Map<String, Long> getMatchCounts();
}
