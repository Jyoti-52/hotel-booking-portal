package com.hotelbooking.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/** Generates short, unguessable references such as HB-7K3M9QXD (no 0/O/1/I to avoid confusion). */
@Component
public class BookingReferenceGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder("HB-");
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
