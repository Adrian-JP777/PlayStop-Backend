package com.playstop.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // ─── PLANTILLA BASE ───────────────────────────────────────────────────────

    private String buildEmail(String content) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background-color:#f0f4f8; font-family:'Segoe UI', Arial, sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f0f4f8; padding:40px 0;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0"
                                   style="max-width:600px; width:100%%; background:#ffffff;
                                          border-radius:16px; overflow:hidden;
                                          box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                                <!-- HEADER -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #1a1a2e 0%%, #16213e 50%%, #0f3460 100%%);
                                               padding:40px 40px 30px; text-align:center;">
                                        <div style="display:inline-block; background:rgba(255,255,255,0.08);
                                                    border-radius:50%%; width:70px; height:70px;
                                                    line-height:70px; font-size:32px; margin-bottom:16px;">
                                            ⚽
                                        </div>
                                        <h1 style="margin:0; color:#ffffff; font-size:28px;
                                                   font-weight:800; letter-spacing:2px;">
                                            Play<span style="color:#e94560;">Stop</span>
                                        </h1>
                                        <p style="margin:6px 0 0; color:rgba(255,255,255,0.6);
                                                  font-size:13px; letter-spacing:1px;">
                                            RESERVAS DEPORTIVAS
                                        </p>
                                    </td>
                                </tr>

                                <!-- CONTENT -->
                                <tr>
                                    <td style="padding:40px;">
                                        %s
                                    </td>
                                </tr>

                                <!-- FOOTER -->
                                <tr>
                                    <td style="background:#f8fafc; border-top:1px solid #e8ecf0;
                                               padding:24px 40px; text-align:center;">
                                        <p style="margin:0; color:#94a3b8; font-size:12px; line-height:1.6;">
                                            Este correo fue enviado automáticamente por PlayStop.<br>
                                            Si no reconoces esta actividad, ignora este mensaje.
                                        </p>
                                        <p style="margin:12px 0 0; color:#cbd5e1; font-size:11px;">
                                            © 2026 PlayStop · Todos los derechos reservados
                                        </p>
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.formatted(content);
    }

    // ─── BADGE DE ESTADO ─────────────────────────────────────────────────────

    private String badge(String color, String text) {
        return """
            <div style="display:inline-block; background:%s;
                        color:#ffffff; font-size:12px; font-weight:700;
                        padding:4px 14px; border-radius:20px;
                        letter-spacing:1px; margin-bottom:20px;">
                %s
            </div>
        """.formatted(color, text);
    }

    // ─── FILA DE DETALLE ─────────────────────────────────────────────────────

    private String detailRow(String icon, String label, String value) {
        return """
            <tr>
                <td style="padding:12px 16px; border-bottom:1px solid #f1f5f9;">
                    <span style="font-size:18px; margin-right:10px;">%s</span>
                    <span style="color:#64748b; font-size:13px; font-weight:600;">%s</span>
                </td>
                <td style="padding:12px 16px; border-bottom:1px solid #f1f5f9;
                           text-align:right; color:#1e293b; font-size:14px; font-weight:600;">
                    %s
                </td>
            </tr>
        """.formatted(icon, label, value);
    }

    // ─── EMAIL: BIENVENIDA ────────────────────────────────────────────────────

    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Bienvenido, %s! 🎉
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Tu cuenta ha sido creada exitosamente. Ya puedes explorar canchas,
                ver disponibilidad y hacer tus reservas en segundos.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#f8fafc; border-radius:12px; margin-bottom:28px;">
                <tr>
                    <td style="padding:20px; text-align:center;">
                        <table cellpadding="0" cellspacing="0" style="margin:0 auto;">
                            <tr>
                                <td style="padding:0 20px; text-align:center;">
                                    <div style="font-size:28px; margin-bottom:6px;">🔍</div>
                                    <p style="margin:0; color:#64748b; font-size:12px; font-weight:600;">
                                        BUSCA
                                    </p>
                                </td>
                                <td style="color:#cbd5e1; font-size:20px;">›</td>
                                <td style="padding:0 20px; text-align:center;">
                                    <div style="font-size:28px; margin-bottom:6px;">📅</div>
                                    <p style="margin:0; color:#64748b; font-size:12px; font-weight:600;">
                                        RESERVA
                                    </p>
                                </td>
                                <td style="color:#cbd5e1; font-size:20px;">›</td>
                                <td style="padding:0 20px; text-align:center;">
                                    <div style="font-size:28px; margin-bottom:6px;">🏃</div>
                                    <p style="margin:0; color:#64748b; font-size:12px; font-weight:600;">
                                        JUEGA
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>

            <p style="margin:0; color:#94a3b8; font-size:13px; text-align:center;">
                ¿Listo para jugar? El equipo de PlayStop te espera. 💪
            </p>
        """.formatted(badge("#22c55e", "✓ CUENTA ACTIVADA"), userName);

        sendHtmlEmail(toEmail, "👋 Bienvenido a PlayStop", buildEmail(content));
    }

    // ─── EMAIL: CONFIRMACIÓN DE RESERVA (con QR adjunto) ─────────────────────

    @Async
    public void sendReservationConfirmationWithQr(String toEmail, String userName,
                                                   String courtName, String date, String slot,
                                                   String reservationId, byte[] qrBytes) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Reserva confirmada! ✅
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, tu reserva está lista.
                Presenta el código QR al ingresar a la instalación.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
                %s
            </table>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="background:#f8fafc; border:1px dashed #cbd5e1;
                          border-radius:16px; margin-bottom:28px;">
                <tr>
                    <td style="padding:28px; text-align:center;">
                        <p style="margin:0 0 16px; color:#475569; font-size:13px;
                                  font-weight:700; text-transform:uppercase; letter-spacing:1px;">
                            Tu código de entrada
                        </p>
                        <img src="cid:qrCode" alt="Código QR de reserva"
                             style="width:200px; height:200px; border-radius:12px;
                                    display:block; margin:0 auto;" />
                        <p style="margin:14px 0 0; color:#94a3b8; font-size:11px; font-family:monospace;">
                            ID: %s
                        </p>
                    </td>
                </tr>
            </table>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:12px;
                        padding:16px 20px; text-align:center;">
                <p style="margin:0; color:#15803d; font-size:13px; font-weight:600;">
                    💡 Recuerda llegar 10 minutos antes — muestra este QR en recepción
                </p>
            </div>
        """.formatted(
            badge("#0ea5e9", "RESERVA CONFIRMADA"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot),
            detailRow("👤", "CLIENTE", userName),
            reservationId
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("✅ Reserva confirmada - PlayStop");
            helper.setText(buildEmail(content), true);
            helper.addInline("qrCode", new ByteArrayResource(qrBytes), "image/png");
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar email con QR: " + e.getMessage());
        }
    }

    // ─── EMAIL: CONFIRMACIÓN DE RESERVA (sin QR — fallback) ──────────────────

    @Async
    public void sendReservationConfirmation(String toEmail, String userName,
                                             String courtName, String date, String slot) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Reserva confirmada! ✅
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, tu reserva está lista.
                Te esperamos en la cancha a la hora indicada.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
            </table>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:12px;
                        padding:16px 20px; text-align:center; margin-bottom:8px;">
                <p style="margin:0; color:#15803d; font-size:13px; font-weight:600;">
                    💡 Recuerda llegar 10 minutos antes de tu reserva
                </p>
            </div>
        """.formatted(
            badge("#0ea5e9", "RESERVA CONFIRMADA"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(toEmail, "✅ Reserva confirmada - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: NUEVA RESERVA (notificación al propietario) ──────────────────

    @Async
    public void sendNewReservationNotificationToOwner(String ownerEmail, String ownerName,
                                                       String clientName, String clientEmail,
                                                       String courtName, String date, String slot,
                                                       String reservationId, double amount) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Nueva reserva recibida 🎉
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, tienes una nueva reserva confirmada
                en tu cancha. Aquí están los detalles:
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
                %s
                %s
            </table>

            <div style="background:linear-gradient(135deg,#f0fdf4,#dcfce7);
                        border:1px solid #bbf7d0; border-radius:12px;
                        padding:16px 20px; text-align:center;">
                <p style="margin:0; color:#15803d; font-size:13px; font-weight:600;">
                    💡 Recuerda tener la cancha lista para recibir al cliente
                </p>
            </div>
        """.formatted(
            badge("#8b5cf6", "🏟️ NUEVA RESERVA"),
            ownerName,
            detailRow("👤", "CLIENTE", clientName),
            detailRow("✉️", "EMAIL", clientEmail),
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(ownerEmail, "🏟️ Nueva reserva en " + courtName + " - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: CANCELACIÓN ───────────────────────────────────────────────────

    @Async
    public void sendReservationCancellation(String toEmail, String userName,
                                             String courtName, String date, String slot) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Reserva cancelada
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, confirmamos que tu reserva ha sido cancelada
                y el reembolso está en proceso.
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
            </table>

            <div style="background:#fff7ed; border:1px solid #fed7aa;
                        border-radius:12px; padding:16px 20px; margin-bottom:8px;">
                <p style="margin:0; color:#c2410c; font-size:13px; font-weight:600;">
                    💳 El reembolso puede tardar entre 3 y 5 días hábiles en reflejarse.
                </p>
            </div>
        """.formatted(
            badge("#ef4444", "RESERVA CANCELADA"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(toEmail, "❌ Reserva cancelada - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: RECORDATORIO ──────────────────────────────────────────────────

    @Async
    public void sendReservationReminder(String toEmail, String userName,
                                         String courtName, String date, String slot) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                ¡Tu reserva es en 1 hora! ⏰
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, este es tu recordatorio.
                ¡Prepárate, es hora de jugar!
            </p>

            <table width="100%%" cellpadding="0" cellspacing="0"
                   style="border:1px solid #e2e8f0; border-radius:12px;
                          overflow:hidden; margin-bottom:28px;">
                %s
                %s
                %s
            </table>

            <div style="background:linear-gradient(135deg,#faf5ff,#ede9fe);
                        border:1px solid #ddd6fe; border-radius:12px;
                        padding:16px 20px; text-align:center;">
                <p style="margin:0; color:#7c3aed; font-size:13px; font-weight:600;">
                    🏃 ¡Calienta motores, te esperamos en la cancha!
                </p>
            </div>
        """.formatted(
            badge("#f59e0b", "⏰ RECORDATORIO"),
            userName,
            detailRow("🏟️", "CANCHA", courtName),
            detailRow("📅", "FECHA", date),
            detailRow("⏰", "HORARIO", slot)
        );

        sendHtmlEmail(toEmail, "⏰ Recordatorio de reserva - PlayStop", buildEmail(content));
    }

    // ─── EMAIL: CÓDIGO DE RECUPERACIÓN ───────────────────────────────────────

    @Async
    public void sendPasswordResetCode(String toEmail, String userName, String code) {
        String content = """
            %s
            <h2 style="margin:0 0 8px; color:#1e293b; font-size:24px; font-weight:700;">
                Recupera tu contraseña
            </h2>
            <p style="margin:0 0 28px; color:#64748b; font-size:15px; line-height:1.7;">
                Hola <strong>%s</strong>, recibimos una solicitud para
                restablecer tu contraseña. Usa el código de abajo.
            </p>

            <div style="background: linear-gradient(135deg,#1a1a2e,#0f3460);
                        border-radius:16px; padding:32px; text-align:center;
                        margin-bottom:28px;">
                <p style="margin:0 0 12px; color:rgba(255,255,255,0.6);
                           font-size:12px; font-weight:700; letter-spacing:2px;">
                    TU CÓDIGO DE VERIFICACIÓN
                </p>
                <div style="display:inline-block; background:rgba(255,255,255,0.08);
                            border:2px solid rgba(233,69,96,0.5);
                            border-radius:12px; padding:16px 40px;">
                    <span style="font-size:42px; font-weight:900; letter-spacing:14px;
                                 color:#ffffff; font-family:monospace;">
                        %s
                    </span>
                </div>
                <p style="margin:16px 0 0; color:rgba(255,255,255,0.5); font-size:12px;">
                    ⏱ Expira en <strong style="color:#e94560;">15 minutos</strong>
                </p>
            </div>

            <div style="background:#fef2f2; border:1px solid #fecaca;
                        border-radius:12px; padding:16px 20px;">
                <p style="margin:0; color:#dc2626; font-size:13px; font-weight:600;">
                    🔒 Si no solicitaste esto, ignora este correo.
                    Tu contraseña no será cambiada.
                </p>
            </div>
        """.formatted(
            badge("#8b5cf6", "🔐 RECUPERACIÓN DE CUENTA"),
            userName,
            code
        );

        sendHtmlEmail(toEmail, "🔐 Código de verificación - PlayStop", buildEmail(content));
    }

    // ─── MÉTODO INTERNO ───────────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error al enviar email: " + e.getMessage());
        }
    }
}