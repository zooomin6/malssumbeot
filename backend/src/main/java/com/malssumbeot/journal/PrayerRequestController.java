package com.malssumbeot.journal;

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

/** 기도 제목 REST 엔드포인트. 로그인한 본인의 기도 제목만 조회·삭제할 수 있다. */
@RestController
@RequestMapping("/api/prayer-requests")
public class PrayerRequestController {

    private final PrayerRequestService service;

    public PrayerRequestController(PrayerRequestService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrayerRequestResponse create(
            @RequestAttribute(name = JwtAuthInterceptor.USER_ID_ATTRIBUTE) String authUserId,
            @Valid @RequestBody PrayerRequestRequest request) {
        return PrayerRequestResponse.from(service.create(Long.valueOf(authUserId), request));
    }

    @GetMapping
    public List<PrayerRequestResponse> list(
            @RequestAttribute(name = JwtAuthInterceptor.USER_ID_ATTRIBUTE) String authUserId) {
        return service.findMine(Long.valueOf(authUserId)).stream().map(PrayerRequestResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute(name = JwtAuthInterceptor.USER_ID_ATTRIBUTE) String authUserId,
            @PathVariable Long id) {
        service.delete(Long.valueOf(authUserId), id);
    }
}
