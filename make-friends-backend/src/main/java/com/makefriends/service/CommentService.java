package com.makefriends.service;

import com.makefriends.dto.CommentDTO;
import com.makefriends.entity.DynamicComment;
import com.makefriends.vo.CommentVO;

import java.util.List;

public interface CommentService {

    DynamicComment addComment(CommentDTO dto);

    boolean deleteComment(Long id);

    List<CommentVO> getCommentsByDynamicId(Long dynamicId);

    CommentVO addCommentAndReturnVO(CommentDTO dto);
}
