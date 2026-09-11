package com.ozerler.marble.service;

import com.ozerler.marble.dto.GlobalSearchHit;
import com.ozerler.marble.dto.GlobalSearchResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.PurchaseOrderRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.SalesOrderRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.SupplierRepository;
import com.ozerler.marble.repository.UserRepository;
import com.ozerler.marble.util.Strings;
import com.ozerler.marble.util.Urls;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private static final int PER_TYPE_LIMIT = 5;
    private static final int MIN_QUERY_LENGTH = 2;

    private final BlockRepository blockRepository;
    private final ProjectRepository projectRepository;
    private final SlabRepository slabRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final CutOrderRepository cutOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final CostCenterRepository costCenterRepository;
    private final CutItemRepository cutItemRepository;
    private final QuarryRepository quarryRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < MIN_QUERY_LENGTH) {
            return GlobalSearchResponse.empty(query);
        }

        Pageable limit = PageRequest.of(0, PER_TYPE_LIMIT);
        List<GlobalSearchHit> hits = new ArrayList<>();
        hits.addAll(searchBlocks(query, limit));
        hits.addAll(searchProjects(query, limit));
        hits.addAll(searchSlabs(query, limit));
        hits.addAll(searchProductionOrders(query, limit));
        hits.addAll(searchCutOrders(query, limit));
        hits.addAll(searchSalesOrders(query, limit));
        hits.addAll(searchPurchaseOrders(query, limit));
        hits.addAll(searchUsers(query, limit));
        hits.addAll(searchCustomers(query, limit));
        hits.addAll(searchSuppliers(query, limit));
        hits.addAll(searchCostCenters(query, limit));
        hits.addAll(searchCutItems(query, limit));
        hits.addAll(searchQuarries(query, limit));

        hits.sort(Comparator.comparingInt(GlobalSearchHit::getScore).reversed()
                .thenComparing(GlobalSearchHit::getTitle, String.CASE_INSENSITIVE_ORDER));

        return GlobalSearchResponse.builder()
                .query(query)
                .total(hits.size())
                .results(hits)
                .build();
    }

    private List<GlobalSearchHit> searchBlocks(String query, Pageable limit) {
        return blockRepository.searchByBlockCode(query, limit).stream()
                .map(block -> hit("BLOCK", "Blok", "box", block.getBlockCode(),
                        Strings.joinDistinct(" · ", block.getStoneType(), statusLabel(block)),
                        "/genealogy?code=" + Urls.encode(block.getBlockCode()), query))
                .toList();
    }

    private List<GlobalSearchHit> searchProjects(String query, Pageable limit) {
        return projectRepository.searchByCodeOrName(query, limit).stream()
                .map(project -> hit("PROJECT", "Şantiye", "building-2",
                        Strings.joinDistinct(" · ", project.getProjectCode(), project.getName()),
                        project.getCustomerName(),
                        "/projects/" + project.getId(), query))
                .toList();
    }

    private List<GlobalSearchHit> searchSlabs(String query, Pageable limit) {
        return slabRepository.searchBySlabCode(query, limit).stream()
                .map(slab -> hit("SLAB", "Plaka", "layers", slab.getSlabCode(),
                        slab.getStatus() != null ? slab.getStatus().name() : null,
                        "/passport/" + Urls.encode(slab.getSlabCode()), query))
                .toList();
    }

    private List<GlobalSearchHit> searchProductionOrders(String query, Pageable limit) {
        return productionOrderRepository.searchByOrderNo(query, limit).stream()
                .map(order -> hit("PRODUCTION", "Kesim emri", "factory", order.getOrderNo(),
                        order.getMachineName(),
                        "/production", query))
                .toList();
    }

    private List<GlobalSearchHit> searchCutOrders(String query, Pageable limit) {
        return cutOrderRepository.searchByCutOrderNo(query, limit).stream()
                .map(order -> {
                    String url = order.getProject() != null
                            ? "/projects/" + order.getProject().getId()
                            : "/workshop";
                    String projectName = order.getProject() != null ? order.getProject().getName() : null;
                    return hit("CUT_ORDER", "Atölye emri", "scissors", order.getCutOrderNo(),
                            projectName, url, query);
                })
                .toList();
    }

    private List<GlobalSearchHit> searchSalesOrders(String query, Pageable limit) {
        return salesOrderRepository.searchByOrderNo(query, limit).stream()
                .map(order -> hit("SALES", "Satış siparişi", "shopping-cart", order.getOrderNo(),
                        order.getCustomer() != null ? order.getCustomer().getCompanyName() : null,
                        "/sales/" + order.getId(), query))
                .toList();
    }

    private List<GlobalSearchHit> searchPurchaseOrders(String query, Pageable limit) {
        return purchaseOrderRepository.searchByPoNumber(query, limit).stream()
                .map(order -> hit("PROCUREMENT", "Satın alma", "truck", order.getPoNumber(),
                        order.getSupplier() != null ? order.getSupplier().getCompanyName() : null,
                        "/procurement/" + order.getId(), query))
                .toList();
    }

    private List<GlobalSearchHit> searchUsers(String query, Pageable limit) {
        return userRepository.searchActiveUsers(query, null, null, limit).getContent().stream()
                .map(user -> hit("USER", "Kullanıcı", "users", user.getFullName(),
                        Strings.joinDistinct(" · ", user.getUsername(), user.getEmail()),
                        "/admin/users/" + user.getId() + "/edit", query))
                .toList();
    }

    private List<GlobalSearchHit> searchCustomers(String query, Pageable limit) {
        return customerRepository.searchByCodeOrName(query, limit).stream()
                .map(customer -> hit("CUSTOMER", "Müşteri", "contact",
                        Strings.joinDistinct(" · ", customer.getCustomerCode(), customer.getCompanyName()),
                        customer.getContactPerson(),
                        "/sales", query))
                .toList();
    }

    private List<GlobalSearchHit> searchSuppliers(String query, Pageable limit) {
        return supplierRepository.searchByCodeOrName(query, limit).stream()
                .map(supplier -> hit("SUPPLIER", "Tedarikçi", "warehouse",
                        Strings.joinDistinct(" · ", supplier.getSupplierCode(), supplier.getCompanyName()),
                        supplier.getContactPerson(),
                        "/procurement", query))
                .toList();
    }

    private List<GlobalSearchHit> searchCostCenters(String query, Pageable limit) {
        return costCenterRepository.searchByCodeOrName(query, limit).stream()
                .map(center -> hit("COST_CENTER", "Masraf merkezi", "calculator",
                        Strings.joinDistinct(" · ", center.getCode(), center.getName()),
                        center.getDescription(),
                        "/costs", query))
                .toList();
    }

    private List<GlobalSearchHit> searchCutItems(String query, Pageable limit) {
        return cutItemRepository.searchByItemCode(query, limit).stream()
                .map(item -> hit("CUT_ITEM", "Mamul", "square", item.getItemCode(),
                        item.getTargetLocation(),
                        "/genealogy?code=" + Urls.encode(item.getItemCode()), query))
                .toList();
    }

    private List<GlobalSearchHit> searchQuarries(String query, Pageable limit) {
        return quarryRepository.searchByCodeOrName(query, limit).stream()
                .map(quarry -> hit("QUARRY", "Ocak", "mountain",
                        Strings.joinDistinct(" · ", quarry.getCode(), quarry.getName()),
                        quarry.getLocation(),
                        "/blocks", query))
                .toList();
    }

    private GlobalSearchHit hit(String type, String typeLabel, String icon,
                                String title, String subtitle, String url, String query) {
        return GlobalSearchHit.builder()
                .type(type)
                .typeLabel(typeLabel)
                .title(title)
                .subtitle(subtitle)
                .url(url)
                .icon(icon)
                .score(score(query, title, subtitle))
                .build();
    }

    private int score(String query, String title, String subtitle) {
        String q = Strings.lowerTurkish(query);
        String t = Strings.lowerTurkish(title);
        String s = Strings.lowerTurkish(subtitle);
        if (t.equals(q)) {
            return 100;
        }
        if (t.startsWith(q)) {
            return 80;
        }
        if (t.contains(q)) {
            return 60;
        }
        if (s.contains(q)) {
            return 40;
        }
        return 10;
    }

    private static String statusLabel(Block block) {
        return block.getStatus() != null ? block.getStatus().name() : null;
    }
}
