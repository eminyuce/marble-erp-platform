package com.ozerler.marble.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cari hesap hareket türleri (Muhasebe Ekstre İşlemleri).
 */
@Getter
@RequiredArgsConstructor
public enum CariTransactionType {

    SATIS_FATURASI("Satış Faturası", true, "Satış faturası ile cari borçlandırılır"),
    ALIS_FATURASI("Alış Faturası", false, "Alış faturası ile cari alacaklandırılır"),
    TAHSILAT("Tahsilat", false, "Nakit, banka, çek vb. tahsilat ile cari alacaklandırılır"),
    ODEME("Ödeme / Tediye", true, "Müşteriye/Cariye ödeme yapılarak cari borçlandırılır"),
    BORC_DEKONTU("Borç Dekontu", true, "Cari hesaba borç kaydedilir"),
    ALACAK_DEKONTU("Alacak Dekontu", false, "Cari hesaba alacak kaydedilir"),
    DEVIR_BAKIYE("Devir / Açılış Bakiyesi", true, "Dönem başı devir bakiyesi"),
    VIRMAN("Virman / Mahsup", true, "Hesaplar arası virman veya mahsup"),
    DIGER("Diğer İşlem", true, "Diğer cari işlem");

    private final String label;
    private final boolean defaultDebit;
    private final String description;
}
