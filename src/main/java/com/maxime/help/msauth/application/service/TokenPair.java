package com.maxime.help.msauth.application.service;

/** An issued access/refresh token pair, returned to the web layer for serialization. */
public record TokenPair(String accessToken, String refreshToken) {}
