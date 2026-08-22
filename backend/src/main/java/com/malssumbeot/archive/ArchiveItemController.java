package com.malssumbeot.archive;

import com.malssumbeot.auth.JwtAuthInterceptor;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 보관함 REST 엔드포인트. 로그인한 본인이 저장한 항목만 조회·삭제할 수 있다. */
@RestController
@RequestMapping("/api/archive-items")
public class ArchiveItemController {

    private final ArchiveItemService service;

    public ArchiveItemController(ArchiveItemService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArchiveItemResponse create(
            @RequestAttribute(name = JwtAuthInterceptor.USER_ID_ATTRIBUTE) String authUserId,
            @Valid @RequestBody ArchiveItemRequest request) {
        return service.create(Long.valueOf(authUserId), request);
    }

    @GetMapping
    public List<ArchiveItemResponse> list(
            @RequestAttribute(name = JwtAuthInterceptor.USER_ID_ATTRIBUTE) String authUserId) {
        return service.findMine(Long.valueOf(authUserId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute(name = JwtAuthInterceptor.USER_ID_ATTRIBUTE) String authUserId,
            @PathVariable Long id) {
        service.delete(Long.valueOf(authUserId), id);
    }
}
