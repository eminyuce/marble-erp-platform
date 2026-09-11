package com.ozerler.marble.model.enums;

import lombok.Getter;

@Getter
public enum ScrapReasonCode {
    FR_01("FR-01", "Kesim Talaş Kaybı", "Lama kalınlığı kaynaklı toz/çamur firesi", "Standart Plaka Maliyetine Eklenir"),
    FR_02("FR-02", "Doğal Taş Kusuru", "Beklenmeyen gizli çatlak/çürük", "Hammadde Sapması / Ocak Seleksiyonu"),
    FR_03("FR-03", "Mekanik Kesim Arızası", "Katrak gönye kaçması, lama kopması", "Makine Bakım & Operasyonel Zarar"),
    FR_04("FR-04", "Kırılma (Taşıma/Vuruntu)", "Vinç veya caraskal aktarımında kırılma", "Fabrika İçi Taşıma Zayiatı"),
    FR_05("FR-05", "Operatör Hatası", "Hatalı ölçü ayarı, aşırı indirme hızı", "Operatör Hata Puanı / Hat Gideri"),
    FR_06("FR-06", "Ölçü Dışı / Çarpıklık", "Plaka gönye sapması, bombe yapması", "Atölyeye İndirimli Yarı Mamul"),
    FR_07("FR-07", "Yüzey İşlem (Cila) Hatası", "File/epoksi hatası, cila kafası yanığı", "Yeniden İşlem (Rework)"),
    FR_08("FR-08", "Renk / Seleksiyon Bozukluğu", "Müşteri toleransı dışına çıkan renk sapması", "Düşük Kalite (C/Moloz) Sınıfı"),
    FR_09("FR-09", "Ebatlama / Nesting Artığı", "Köprü kesmede sipariş dışı kenar artığı", "Mozaik / Kırma Taş Stoğu"),
    FR_10("FR-10", "Şantiye Kırığı / Montaj Zayiatı", "Şantiye indirme, taşıma veya montaj kırım", "Proje Şantiye Maliyeti");

    private final String code;
    private final String title;
    private final String description;
    private final String costImpact;

    ScrapReasonCode(String code, String title, String description, String costImpact) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.costImpact = costImpact;
    }

    public static ScrapReasonCode fromCode(String code) {
        if (code == null) return null;
        for (ScrapReasonCode r : values()) {
            if (r.getCode().equalsIgnoreCase(code) || r.name().equalsIgnoreCase(code)) {
                return r;
            }
        }
        return null;
    }
}
