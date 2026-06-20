package com.lunar.lunar_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lunar.lunar_backend.dto.NoteResponse;
import com.lunar.lunar_backend.dto.NoteSaveRequest;
import com.lunar.lunar_backend.entity.Note;
import java.util.List;

public interface NoteService extends IService<Note> {

    NoteResponse saveNote(Long userId, NoteSaveRequest request);

    List<NoteResponse> listNotes(Long userId, String birthTime);

    void deleteNote(Long userId, Long noteId);

    void clearNotes(Long userId, String birthTime);
}
