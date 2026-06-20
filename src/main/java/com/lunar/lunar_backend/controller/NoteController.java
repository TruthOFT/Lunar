package com.lunar.lunar_backend.controller;

import com.lunar.lunar_backend.common.ApiResponse;
import com.lunar.lunar_backend.common.AuthContext;
import com.lunar.lunar_backend.common.AuthUser;
import com.lunar.lunar_backend.dto.NoteResponse;
import com.lunar.lunar_backend.dto.NoteSaveRequest;
import com.lunar.lunar_backend.service.NoteService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notes")
public class NoteController {

    @Resource
    private NoteService noteService;

    @PostMapping
    public ApiResponse<NoteResponse> save(@RequestBody NoteSaveRequest requestBody, HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        return ApiResponse.success(noteService.saveNote(authUser.userId(), requestBody));
    }

    @GetMapping
    public ApiResponse<List<NoteResponse>> list(
            @RequestParam(required = false) String birthTime,
            HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        return ApiResponse.success(noteService.listNotes(authUser.userId(), birthTime));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        noteService.deleteNote(authUser.userId(), id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/clear")
    public ApiResponse<Void> clear(@RequestParam String birthTime, HttpServletRequest request) {
        AuthUser authUser = (AuthUser) request.getAttribute(AuthContext.REQUEST_ATTRIBUTE);
        noteService.clearNotes(authUser.userId(), birthTime);
        return ApiResponse.success(null);
    }
}
