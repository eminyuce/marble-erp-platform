package com.ozerler.marble.service;

import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.MaterialLot;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.PalletItem;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Shipment;
import com.ozerler.marble.model.enums.MaterialLotStatus;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.MaterialLotRepository;
import com.ozerler.marble.repository.PalletItemRepository;
import com.ozerler.marble.repository.PalletLocationMovementRepository;
import com.ozerler.marble.repository.PalletRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.ShipmentItemRepository;
import com.ozerler.marble.repository.ShipmentRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PalletShipmentServiceTest {

    @Mock
    private PalletRepository palletRepository;
    @Mock
    private PalletItemRepository palletItemRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ShipmentItemRepository shipmentItemRepository;
    @Mock
    private MaterialLotRepository materialLotRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private StockLocationRepository stockLocationRepository;
    @Mock
    private PalletLocationMovementRepository palletLocationMovementRepository;

    @InjectMocks
    private PalletShipmentService palletShipmentService;

    @Test
    @DisplayName("Müşteri veya şantiye olmadan palet oluşturulursa hata fırlatmalı")
    void createPallet_ThrowsWhenNoCustomerAndNoProject() {
        assertThatThrownBy(() -> palletShipmentService.createPallet(null, null, null, "A-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Müşteri ile palet başarıyla oluşturulmalı")
    void createPallet_Success() {
        Customer customer = Customer.builder().id(1L).companyName("Test Müşteri").build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(palletRepository.save(any(Pallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pallet pallet = palletShipmentService.createPallet("PAL-001", 1L, null, "Saha 1");

        assertThat(pallet).isNotNull();
        assertThat(pallet.getPalletCode()).isEqualTo("PAL-001");
        assertThat(pallet.getStatus()).isEqualTo("PREPARING");
        assertThat(pallet.getCustomer()).isEqualTo(customer);
    }

    @Test
    @DisplayName("Palete lot eklenince palet durumu READY olmalı")
    void addLot_Success() {
        Pallet pallet = Pallet.builder().id(10L).status("PREPARING").build();
        MaterialLot lot = MaterialLot.builder().id(20L).lotCode("LOT-100").totalAreaM2(new BigDecimal("15.50")).build();

        when(palletRepository.findById(10L)).thenReturn(Optional.of(pallet));
        when(materialLotRepository.findById(20L)).thenReturn(Optional.of(lot));
        when(palletItemRepository.save(any(PalletItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PalletItem item = palletShipmentService.addLot(10L, 20L, 2, new BigDecimal("15.50"));

        assertThat(item).isNotNull();
        assertThat(pallet.getStatus()).isEqualTo("READY");
        assertThat(lot.getStatus()).isEqualTo(MaterialLotStatus.PALLETIZED);
    }

    @Test
    @DisplayName("Sevk edilmiş palete lot eklenmeye çalışılırsa IllegalStateException fırlatmalı")
    void addLot_ThrowsWhenPalletShipped() {
        Pallet pallet = Pallet.builder().id(10L).status("SHIPPED").build();
        when(palletRepository.findById(10L)).thenReturn(Optional.of(pallet));

        assertThatThrownBy(() -> palletShipmentService.addLot(10L, 20L, 1, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Sevk edilmiş palete");
    }

    @Test
    @DisplayName("Sevkiyat oluşturulunca palet durumu SHIPPED olmalı")
    void createShipment_Success() {
        Pallet pallet = Pallet.builder().id(10L).status("READY").build();
        Customer customer = Customer.builder().id(1L).companyName("Hedef Müşteri").build();
        PalletItem item = PalletItem.builder().id(100L).pallet(pallet)
                .materialLot(MaterialLot.builder().id(50L).status(MaterialLotStatus.PALLETIZED).build()).build();

        when(palletRepository.findById(10L)).thenReturn(Optional.of(pallet));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(palletItemRepository.findByPalletId(10L)).thenReturn(List.of(item));

        Shipment shipment = palletShipmentService.createShipment(10L, 1L, null, "IRS-999", "03 AA 001", "Ahmet", BigDecimal.valueOf(500));

        assertThat(shipment).isNotNull();
        assertThat(pallet.getStatus()).isEqualTo("SHIPPED");
        assertThat(item.getMaterialLot().getStatus()).isEqualTo(MaterialLotStatus.SHIPPED);
        verify(shipmentItemRepository).save(any());
    }

    @Test
    @DisplayName("Pallet listesi çekilirken items'lar paletlere atanmalı")
    void pallets_PopulatesItems() {
        Pallet p1 = Pallet.builder().id(1L).status("PREPARING").build();
        Pallet p2 = Pallet.builder().id(2L).status("SHIPPED").build();
        List<Pallet> palletList = new ArrayList<>(List.of(p1, p2));

        PalletItem item1 = PalletItem.builder().id(101L).pallet(p1).areaM2(new BigDecimal("5.00")).quantity(2).build();
        PalletItem item2 = PalletItem.builder().id(102L).pallet(p1).areaM2(new BigDecimal("7.50")).quantity(3).build();

        when(palletRepository.findAllWithLocationAndSlabs()).thenReturn(palletList);
        when(palletItemRepository.findByPalletIn(palletList)).thenReturn(List.of(item1, item2));

        List<Pallet> result = palletShipmentService.pallets();

        assertThat(result).hasSize(2);
        assertThat(p1.getItems()).containsExactly(item1, item2);
        assertThat(p1.getTotalLotCount()).isEqualTo(2);
        assertThat(p1.getTotalQuantity()).isEqualTo(5);
        assertThat(p1.getTotalAreaM2()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(p2.getItems()).isEmpty();
    }

    @Test
    @DisplayName("shippablePallets sadece sevk edilmemiş paletleri dönmeli")
    void shippablePallets_FiltersOutShipped() {
        Pallet p1 = Pallet.builder().id(1L).status("READY").build();
        Pallet p2 = Pallet.builder().id(2L).status("SHIPPED").build();
        when(palletRepository.findAllWithLocationAndSlabs()).thenReturn(List.of(p1, p2));

        List<Pallet> shippable = palletShipmentService.shippablePallets();

        assertThat(shippable).hasSize(1);
        assertThat(shippable.get(0).getId()).isEqualTo(1L);
    }
}
