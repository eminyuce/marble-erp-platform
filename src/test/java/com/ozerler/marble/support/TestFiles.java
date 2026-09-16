package com.ozerler.marble.support;

import java.nio.charset.StandardCharsets;

public final class TestFiles {

    private TestFiles() {
    }

    public static byte[] jpeg() {
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01, 0x01, 0x00
        };
    }

    public static byte[] png() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R'
        };
    }

    public static byte[] gif() {
        return "GIF89a........".getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] webp() {
        return new byte[]{
                'R', 'I', 'F', 'F', 0x10, 0x00, 0x00, 0x00,
                'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };
    }

    public static byte[] pdf() {
        return "%PDF-1.4\n%fake-pdf".getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] zipOffice() {
        return new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00};
    }

    public static byte[] svg() {
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
                  <rect width="10" height="10" fill="blue"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] maliciousSvg() {
        return """
                <svg xmlns="http://www.w3.org/2000/svg">
                  <script>alert('xss')</script>
                </svg>
                """.getBytes(StandardCharsets.UTF_8);
    }
}
