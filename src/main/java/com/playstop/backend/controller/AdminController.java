package com.playstop.backend.controller;

import com.playstop.backend.entity.Court;
import com.playstop.backend.entity.Reservation;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.ReservationStatus;
import com.playstop.backend.enums.Role;
import com.playstop.backend.repository.CourtRepository;
import com.playstop.backend.repository.ReservationRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;

    // ── STATS (enriquecido) ──────────────────────────────────────────────────

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers",        userRepository.countByRole(Role.USER));
        stats.put("totalOwners",       userRepository.countByRole(Role.OWNER));
        stats.put("totalCourts",       courtRepository.count());
        stats.put("totalReservations", reservationRepository.count());

        // Desglose por estado
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ReservationStatus s : ReservationStatus.values()) {
            byStatus.put(s.name(), reservationRepository.countByStatus(s));
        }
        stats.put("byStatus", byStatus);

        // Top 5 canchas más reservadas
        List<Reservation> all = reservationRepository.findAll();
        List<Map<String, Object>> topCourts = all.stream()
            .collect(Collectors.groupingBy(r -> r.getCourt().getName(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", e.getKey());
                m.put("count", e.getValue());
                return m;
            })
            .toList();
        stats.put("topCourts", topCourts);

        // Últimos 5 jugadores registrados
        List<Map<String, Object>> recentUsers = userRepository
            .findTop5ByRoleOrderByCreatedAtDesc(Role.USER)
            .stream()
            .map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name",      u.getName());
                m.put("email",     u.getEmail());
                m.put("createdAt", u.getCreatedAt().toString());
                return m;
            })
            .toList();
        stats.put("recentUsers", recentUsers);

        return ResponseEntity.ok(stats);
    }

    // ── ANALYTICS ────────────────────────────────────────────────────────────

    @GetMapping("/analytics")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        List<Reservation> all = reservationRepository.findAll();

        // Últimos 6 meses
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM").withLocale(new java.util.Locale("es", "PE"));
        List<Map<String, Object>> monthly = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            long count = all.stream().filter(r -> YearMonth.from(r.getDate()).equals(ym)).count();
            double revenue = all.stream()
                .filter(r -> YearMonth.from(r.getDate()).equals(ym) && r.getStatus() != ReservationStatus.CANCELLED)
                .mapToDouble(r -> r.getTotalAmount().doubleValue()).sum();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label",   ym.format(fmt).toUpperCase());
            m.put("count",   count);
            m.put("revenue", Math.round(revenue * 100.0) / 100.0);
            monthly.add(m);
        }

        // Distribución por deporte
        Map<String, Long> sportDist = all.stream()
            .collect(Collectors.groupingBy(r -> r.getCourt().getSportType(), Collectors.counting()));

        // Ingreso total (excluye canceladas)
        double totalRevenue = all.stream()
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
            .mapToDouble(r -> r.getTotalAmount().doubleValue()).sum();
        totalRevenue = Math.round(totalRevenue * 100.0) / 100.0;

        // Actividad reciente (últimas 20 reservas)
        List<Map<String, Object>> recent = all.stream()
            .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
            .limit(20)
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("clientName", r.getUser().getName());
                m.put("courtName",  r.getCourt().getName());
                m.put("status",     r.getStatus().name());
                m.put("amount",     r.getTotalAmount());
                m.put("date",       r.getDate().toString());
                m.put("createdAt",  r.getCreatedAt().toString());
                return m;
            }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthly",           monthly);
        result.put("sportDistribution", sportDist);
        result.put("totalRevenue",      totalRevenue);
        result.put("recentActivity",    recent);
        return ResponseEntity.ok(result);
    }

    // ── JUGADORES ────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> result = userRepository.findByRole(Role.USER).stream()
            .map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",        u.getId().toString());
                m.put("name",      u.getName());
                m.put("email",     u.getEmail());
                m.put("phone",     u.getPhone() != null ? u.getPhone() : "");
                m.put("enabled",   u.isEnabled());
                m.put("createdAt", u.getCreatedAt().toString());
                return m;
            }).toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/users/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("id", user.getId().toString(), "enabled", user.isEnabled()));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getRole() == Role.OWNER) {
            // Desactivar canchas en lugar de eliminarlas (preserva historial)
            courtRepository.findByOwner(user).forEach(c -> {
                c.setActive(false);
                courtRepository.save(c);
            });
            // Anonimizar cuenta del propietario
            user.setEnabled(false);
            user.setName("[Cuenta eliminada]");
            user.setEmail("deleted_" + user.getId() + "@playstop.internal");
            userRepository.save(user);
        } else {
            // Para jugadores: eliminar sus reservas y luego la cuenta
            reservationRepository.deleteAll(reservationRepository.findByUser(user));
            userRepository.delete(user);
        }
        return ResponseEntity.noContent().build();
    }

    // ── PROPIETARIOS ─────────────────────────────────────────────────────────

    @GetMapping("/owners")
    public ResponseEntity<List<Map<String, Object>>> getAllOwners() {
        List<Map<String, Object>> result = userRepository.findByRole(Role.OWNER).stream()
            .filter(o -> !o.getEmail().endsWith("@playstop.internal"))
            .map(o -> {
                long courts = courtRepository.findByOwner(o).size();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",        o.getId().toString());
                m.put("name",      o.getName());
                m.put("email",     o.getEmail());
                m.put("phone",     o.getPhone() != null ? o.getPhone() : "");
                m.put("enabled",   o.isEnabled());
                m.put("courts",    courts);
                m.put("createdAt", o.getCreatedAt().toString());
                return m;
            }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/owners/{id}/courts")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getOwnerCourts(@PathVariable UUID id) {
        User owner = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Propietario no encontrado"));
        List<Map<String, Object>> result = courtRepository.findByOwner(owner).stream()
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           c.getId().toString());
                m.put("name",         c.getName());
                m.put("sportType",    c.getSportType());
                m.put("pricePerHour", c.getPricePerHour());
                m.put("city",         c.getCity() != null ? c.getCity() : "");
                m.put("district",     c.getDistrict() != null ? c.getDistrict() : "");
                m.put("active",       c.isActive());
                return m;
            }).toList();
        return ResponseEntity.ok(result);
    }

    // ── CANCHAS ──────────────────────────────────────────────────────────────

    @GetMapping("/courts")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllCourts() {
        List<Map<String, Object>> result = courtRepository.findAll().stream()
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           c.getId().toString());
                m.put("name",         c.getName());
                m.put("sportType",    c.getSportType());
                m.put("pricePerHour", c.getPricePerHour());
                m.put("ownerName",    c.getOwner().getName());
                m.put("ownerEmail",   c.getOwner().getEmail());
                m.put("city",         c.getCity() != null ? c.getCity() : "");
                m.put("district",     c.getDistrict() != null ? c.getDistrict() : "");
                m.put("active",       c.isActive());
                m.put("createdAt",    c.getCreatedAt().toString());
                return m;
            }).toList();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/courts/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleCourtStatus(@PathVariable UUID id) {
        Court court = courtRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cancha no encontrada"));
        court.setActive(!court.isActive());
        courtRepository.save(court);
        return ResponseEntity.ok(Map.of("id", court.getId().toString(), "active", court.isActive()));
    }

    // ── RESERVAS ─────────────────────────────────────────────────────────────

    @GetMapping("/all-reservations")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getAllReservations() {
        List<Map<String, Object>> result = reservationRepository.findAll().stream()
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",          r.getId().toString());
                m.put("clientName",  r.getUser().getName());
                m.put("clientEmail", r.getUser().getEmail());
                m.put("courtName",   r.getCourt().getName());
                m.put("ownerName",   r.getCourt().getOwner().getName());
                m.put("date",        r.getDate().toString());
                m.put("slot",        String.format("%02d:00 - %02d:00", r.getSlotHour(), r.getSlotHour() + 1));
                m.put("status",      r.getStatus().name());
                m.put("amount",      r.getTotalAmount());
                m.put("createdAt",   r.getCreatedAt().toString());
                return m;
            }).toList();
        return ResponseEntity.ok(result);
    }
}
