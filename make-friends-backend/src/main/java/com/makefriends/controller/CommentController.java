package com.makefriends.controller;

import com.makefriends.common.Result;
import com.makefriends.dto.CommentDTO;
import com.makefriends.entity.DynamicComment;
import com.makefriends.service.CommentService;
import com.makefriends.vo.CommentVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public Result<CommentVO> addComment(@Valid @RequestBody CommentDTO dto) {
        return Result.ok(commentService.addCommentAndReturnVO(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.ok();
    }

    @GetMapping("/list/{dynamicId}")
    public Result<List<CommentVO>> getCommentsByDynamicId(@PathVariable Long dynamicId) {
        return Result.ok(commentService.getCommentsByDynamicId(dynamicId));
    }
}