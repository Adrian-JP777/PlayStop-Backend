package com.playstop.backend.controller;

import com.playstop.backend.dto.request.ReservationRequest;
import com.playstop.backend.dto.response.ReservationResponse;
import com.playstop.backend.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservation(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations() {
        return ResponseEntity.ok(reservationService.getMyReservations());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }

    @GetMapping("/court/{courtId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByCourt(
            @PathVariable UUID courtId) {
        return ResponseEntity.ok(reservationService.getReservationsByCourt(courtId));
    }

    @PatchMapping("/{id}/cancel-by-owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReservationResponse> cancelReservationByOwner(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.cancelReservationByOwner(id));
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<byte[]> getReservationQr(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(reservationService.getReservationQr(id));
    }

    @GetMapping("/verify/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReservationResponse> verifyReservation(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.verifyReservation(id));
    }

    @PatchMapping("/{id}/confirm-attendance")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ReservationResponse> confirmAttendance(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.confirmAttendance(id));
    }
}