package com.makefriends.service;

public interface LikeService {

    boolean toggleLike(Long dynamicId);

    boolean checkLiked(Long dynamicId);
}
