package com.lunar.lunar_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lunar.lunar_backend.common.ErrorCode;
import com.lunar.lunar_backend.dto.NoteResponse;
import com.lunar.lunar_backend.dto.NoteSaveRequest;
import com.lunar.lunar_backend.entity.Note;
import com.lunar.lunar_backend.exception.ApiException;
import com.lunar.lunar_backend.mapper.NoteMapper;
import com.lunar.lunar_backend.service.NoteService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public NoteResponse saveNote(Long userId, NoteSaveRequest request) {
        if (!StringUtils.hasText(request.content())) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        if (!StringUtils.hasText(request.birthTime())) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        Note note = new Note();
        note.setUserId(userId);
        note.setBirthTime(request.birthTime().trim());
        note.setEventTime(request.eventTime() == null ? "" : request.eventTime().trim());
        note.setContent(request.content().trim());
        save(note);
        return toResponse(note);
    }

    @Override
    public List<NoteResponse> listNotes(Long userId, String birthTime) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .orderByAsc(Note::getCreateTime);
        if (StringUtils.hasText(birthTime)) {
            wrapper.eq(Note::getBirthTime, birthTime.trim());
        }
        return list(wrapper).stream().map(this::toResponse).toList();
    }

    @Override
    public void deleteNote(Long userId, Long noteId) {
        Note note = getById(noteId);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        removeById(noteId);
    }

    @Override
    public void clearNotes(Long userId, String birthTime) {
        if (!StringUtils.hasText(birthTime)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        remove(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(Note::getBirthTime, birthTime.trim()));
    }

    private NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getBirthTime(),
                note.getEventTime() == null ? "" : note.getEventTime(),
                note.getContent(),
                note.getCreateTime() == null ? "" : DATE_TIME_FORMATTER.format(note.getCreateTime())
        );
    }
}
