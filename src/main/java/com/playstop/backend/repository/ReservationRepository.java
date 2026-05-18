package com.playstop.backend.repository;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByUser(User user);

    List<Reservation> findByCourt(Court court);

    @Query("SELECT r.slotHour FROM Reservation r WHERE r.court = :court AND r.date = :date AND r.status != :cancelled")
    List<Integer> findOccupiedSlots(Court court, LocalDate date, ReservationStatus cancelled);

    boolean existsByCourtAndDateAndSlotHourAndStatusNot(
        Court court, LocalDate date, int slotHour, ReservationStatus status
    );

    // ✅ Nuevo — usado por ReminderScheduler
    List<Reservation> findByDateAndSlotHourAndStatus(
        LocalDate date, int slotHour, ReservationStatus status
    );

    long countByStatus(ReservationStatus status);

    List<Reservation> findByUser_Role(com.playstop.backend.enums.Role role);
}