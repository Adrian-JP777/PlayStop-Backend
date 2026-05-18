package com.playstop.backend.service;

import com.playstop.backend.dto.request.ReservationRequest;
import com.playstop.backend.dto.response.ReservationResponse;
import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final QrService qrService;

    public ReservationResponse createReservation(ReservationRequest request) {
        User user = getCurrentUser();

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));

        if (!court.isActive()) {
            throw new RuntimeException("La cancha no está disponible");
        }

        boolean slotTaken = reservationRepository.existsByCourtAndDateAndSlotHourAndStatusNot(
                court, request.getDate(), request.getSlotHour(), ReservationStatus.CANCELLED
        );

        if (slotTaken) {
            throw new RuntimeException("Ese horario ya está reservado");
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .court(court)
                .date(request.getDate())
                .slotHour(request.getSlotHour())
                .totalAmount(court.getPricePerHour())
                .status(ReservationStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        String slot = String.format("%02d:00 - %02d:00", saved.getSlotHour(), saved.getSlotHour() + 1);

        // Generate QR and send enriched confirmation email; fall back to plain email on error
        try {
            String qrContent = String.format(
                "PLAYSTOP|ID:%s|CANCHA:%s|FECHA:%s|HORA:%s|CLIENTE:%s|MONTO:S/ %.2f",
                saved.getId(), court.getName(), saved.getDate(), slot,
                user.getName(), saved.getTotalAmount()
            );
            byte[] qrBytes = qrService.generateQr(qrContent);
            emailService.sendReservationConfirmationWithQr(
                user.getEmail(), user.getName(), court.getName(),
                saved.getDate().toString(), slot, saved.getId().toString(), qrBytes
            );
        } catch (Exception ex) {
            log.error("QR generation failed for reservation {}, sending plain email: {}", saved.getId(), ex.getMessage());
            emailService.sendReservationConfirmation(
                user.getEmail(), user.getName(), court.getName(),
                saved.getDate().toString(), slot
            );
        }

        // Notify the court owner about the new booking
        emailService.sendNewReservationNotificationToOwner(
            court.getOwner().getEmail(), court.getOwner().getName(),
            user.getName(), user.getEmail(),
            court.getName(), saved.getDate().toString(), slot,
            saved.getId().toString(), saved.getTotalAmount().doubleValue()
        );

        return toResponse(saved);
    }

    public List<ReservationResponse> getMyReservations() {
        User user = getCurrentUser();
        return reservationRepository.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ReservationResponse> getReservationsByCourt(UUID courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));

        User owner = getCurrentUser();
        if (!court.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("No tienes permiso para ver estas reservas");
        }

        return reservationRepository.findByCourt(court)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservationResponse cancelReservation(UUID reservationId) {
        User user = getCurrentUser();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para cancelar esta reserva");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("La reserva ya está cancelada");
        }

        LocalDateTime reservationDateTime = reservation.getDate()
                .atTime(reservation.getSlotHour(), 0);

        if (LocalDateTime.now().isAfter(reservationDateTime.minusHours(24))) {
            throw new RuntimeException("Solo puedes cancelar hasta 24 horas antes de la reserva");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        // ✅ Email de cancelación
        emailService.sendReservationCancellation(
            user.getEmail(),
            user.getName(),
            reservation.getCourt().getName(),
            reservation.getDate().toString(),
            String.format("%02d:00 - %02d:00", reservation.getSlotHour(), reservation.getSlotHour() + 1)
        );

        return toResponse(saved);
    }

    public ReservationResponse cancelReservationByOwner(UUID reservationId) {
        User owner = getCurrentUser();

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reservation.getCourt().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("No tienes permiso para cancelar esta reserva");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("La reserva ya está cancelada");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        emailService.sendReservationCancellation(
            reservation.getUser().getEmail(),
            reservation.getUser().getName(),
            reservation.getCourt().getName(),
            reservation.getDate().toString(),
            String.format("%02d:00 - %02d:00", reservation.getSlotHour(), reservation.getSlotHour() + 1)
        );

        return toResponse(saved);
    }

    public ReservationResponse getReservationById(UUID id) {
        User user = getCurrentUser();
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para ver esta reserva");
        }

        return toResponse(reservation);
    }

    public byte[] getReservationQr(UUID reservationId) {
        User user = getCurrentUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso");
        }

        String slot = String.format("%02d:00 - %02d:00", reservation.getSlotHour(), reservation.getSlotHour() + 1);
        String qrContent = String.format(
            "PLAYSTOP|ID:%s|CANCHA:%s|FECHA:%s|HORA:%s|CLIENTE:%s|MONTO:S/ %.2f",
            reservation.getId(), reservation.getCourt().getName(),
            reservation.getDate(), slot,
            reservation.getUser().getName(), reservation.getTotalAmount()
        );
        try {
            return qrService.generateQr(qrContent);
        } catch (Exception e) {
            throw new RuntimeException("Error generando QR: " + e.getMessage());
        }
    }

    public ReservationResponse verifyReservation(UUID reservationId) {
        User owner = getCurrentUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reservation.getCourt().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("Esta reserva no pertenece a ninguna de tus canchas");
        }

        return toResponse(reservation);
    }

    public ReservationResponse confirmAttendance(UUID reservationId) {
        User owner = getCurrentUser();
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reservation.getCourt().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("No tienes permiso para confirmar esta reserva");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("La reserva está cancelada");
        }

        if (reservation.getStatus() == ReservationStatus.ATTENDED) {
            throw new RuntimeException("La asistencia ya fue confirmada anteriormente");
        }

        reservation.setStatus(ReservationStatus.ATTENDED);
        return toResponse(reservationRepository.save(reservation));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private ReservationResponse toResponse(Reservation r) {
        String slotLabel = String.format("%02d:00 - %02d:00", r.getSlotHour(), r.getSlotHour() + 1);
        return ReservationResponse.builder()
                .id(r.getId())
                .courtName(r.getCourt().getName())
                .sportType(r.getCourt().getSportType())
                .courtAddress(r.getCourt().getAddress())
                .date(r.getDate())
                .slotHour(r.getSlotHour())
                .slotLabel(slotLabel)
                .totalAmount(r.getTotalAmount())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .clientName(r.getUser().getName())
                .clientEmail(r.getUser().getEmail())
                .build();
    }
}