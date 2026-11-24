package com.ozerler.marble.dto;

/**
 * What the setup page shows: the QR image, the manual key and the account label.
 *
 * @param accountName label in the authenticator app
 * @param manualKey Base32 secret grouped in fours for typing
 * @param qrCodeBase64 PNG image
 */
public record TwoFactorSetupView(String accountName, String manualKey, String qrCodeBase64) {
}
