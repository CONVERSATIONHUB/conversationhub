package com.example.conversationHub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingService bookingService;

    public BookingExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${booking.expiry.scan-ms:60000}")
    public void expirePendingRequests() {
        int expired = bookingService.expirePendingBookings();
        if (expired > 0) {
            log.info("Expired {} pending booking request(s)", expired);
        }
    }
}
