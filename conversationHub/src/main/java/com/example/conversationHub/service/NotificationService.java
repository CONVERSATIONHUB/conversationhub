package com.example.conversationHub.service;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.domain.Booking;
import com.example.conversationHub.domain.LanguagePreference;
import com.example.conversationHub.domain.Notification;
import com.example.conversationHub.domain.enums.LanguageCode;
import com.example.conversationHub.repository.AppUserRepository;
import com.example.conversationHub.repository.LanguagePreferenceRepository;
import com.example.conversationHub.repository.NotificationRepository;
import com.example.conversationHub.web.dto.NotificationView;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final LanguagePreferenceRepository languagePreferenceRepository;
    private final AppUserRepository userRepository;
    private final MessageSource messageSource;

    public NotificationService(NotificationRepository notificationRepository,
                               LanguagePreferenceRepository languagePreferenceRepository,
                               AppUserRepository userRepository,
                               MessageSource messageSource) {
        this.notificationRepository = notificationRepository;
        this.languagePreferenceRepository = languagePreferenceRepository;
        this.userRepository = userRepository;
        this.messageSource = messageSource;
    }

    public void notify(AppUser user, Booking booking, String messageKey) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setBooking(booking);
        notification.setMessageKey(messageKey);
        notification.setLanguageCode(preferredLanguage(user));
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> listMine(String username) {
        AppUser user = currentUser(username);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        AppUser user = currentUser(username);
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    public void markRead(Long notificationId, String username) {
        AppUser user = currentUser(username);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public int markAllRead(String username) {
        AppUser user = currentUser(username);
        List<Notification> unread =
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    private LanguageCode preferredLanguage(AppUser user) {
        return languagePreferenceRepository.findByUserId(user.getId())
                .map(LanguagePreference::getLanguageCode)
                .orElse(LanguageCode.EN);
    }

    private NotificationView toView(Notification notification) {
        String message = messageSource.getMessage(
                notification.getMessageKey(),
                null,
                notification.getMessageKey(),
                localeFor(notification.getLanguageCode())
        );
        return new NotificationView(
                notification.getId(),
                message,
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getBooking() == null ? null : notification.getBooking().getId()
        );
    }

    private Locale localeFor(LanguageCode code) {
        return code == LanguageCode.SW ? new Locale("sw") : Locale.ENGLISH;
    }

    private AppUser currentUser(String username) {
        return userRepository.findByPhone(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}
