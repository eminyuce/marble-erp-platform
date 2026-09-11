package com.ozerler.marble.repository;

import com.ozerler.marble.model.Pallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PalletRepository extends JpaRepository<Pallet, Long> {
    Optional<Pallet> findByPalletCode(String palletCode);
    Optional<Pallet> findByQrCodeHash(String qrCodeHash);
}
