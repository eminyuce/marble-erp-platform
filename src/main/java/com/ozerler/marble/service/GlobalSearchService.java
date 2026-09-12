package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

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
    private final org.springframework.context.MessageSource messageSource;

    @Qualifier(Constants.SEARCH_EXECUTOR)
    private final AsyncTaskExecutor searchTaskExecutor;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    public GlobalSearchResponse search(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < Constants.SEARCH_MIN_QUERY_LENGTH) {
            return GlobalSearchResponse.empty(query);
        }

        Pageable limit = PageRequest.of(0, Constants.SEARCH_PER_TYPE_LIMIT);
        List<GlobalSearchHit> hits = List.of(
                        supplyAsync(() -> searchBlocks(query, limit)),
                        supplyAsync(() -> searchProjects(query, limit)),
                        supplyAsync(() -> searchSlabs(query, limit)),
                        supplyAsync(() -> searchProductionOrders(query, limit)),
                        supplyAsync(() -> searchCutOrders(query, limit)),
                        supplyAsync(() -> searchSalesOrders(query, limit)),
                        supplyAsync(() -> searchPurchaseOrders(query, limit)),
                        supplyAsync(() -> searchUsers(query, limit)),
                        supplyAsync(() -> searchCustomers(query, limit)),
                        supplyAsync(() -> searchSuppliers(query, limit)),
                        supplyAsync(() -> searchCostCenters(query, limit)),
                        supplyAsync(() -> searchCutItems(query, limit)),
                        supplyAsync(() -> searchQuarries(query, limit)))
                .stream()
                .map(this::joinHits)
                .flatMap(List::stream)
                .collect(Collectors.toCollection(ArrayList::new));

        hits.sort(Comparator.comparingInt(GlobalSearchHit::getScore).reversed()
                .thenComparing(GlobalSearchHit::getTitle, String.CASE_INSENSITIVE_ORDER));

        return GlobalSearchResponse.builder()
                .query(query)
                .total(hits.size())
                .results(hits)
                .build();
    }

    private CompletableFuture<List<GlobalSearchHit>> supplyAsync(Supplier<List<GlobalSearchHit>> search) {
        return CompletableFuture.supplyAsync(search, searchTaskExecutor);
    }

    private List<GlobalSearchHit> joinHits(CompletableFuture<List<GlobalSearchHit>> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(getMessage("error.search.failed"), cause);
        }
    }

    private List<GlobalSearchHit> searchBlocks(String query, Pageable limit) {
        return blockRepository.searchByBlockCode(query, limit).stream()
                .map(block -> hit("BLOCK", getMessage("search.type.block"), "box", block.getBlockCode(),
                        Strings.joinDistinct(" · ", block.getStoneType(), statusLabel(block)),
                        "/genealogy?code=" + Urls.encode(block.getBlockCode()), query))
                .toList();
    }

    private List<GlobalSearchHit> searchProjects(String query, Pageable limit) {
        return projectRepository.searchByCodeOrName(query, limit).stream()
                .map(project -> hit("PROJECT", getMessage("search.type.project"), "building-2",
                        Strings.joinDistinct(" · ", project.getProjectCode(), project.getName()),
                        project.getCustomerName(),
                        "/projects/" + project.getId(), query))
                .toList();
    }

    private List<GlobalSearchHit> searchSlabs(String query, Pageable limit) {
        return slabRepository.searchBySlabCode(query, limit).stream()
                .map(slab -> hit("SLAB", getMessage("search.type.slab"), "layers", slab.getSlabCode(),
                        slab.getStatus() != null ? slab.getStatus().name() : null,
                        "/passport/" + Urls.encode(slab.getSlabCode()), query))
                .toList();
    }

    private List<GlobalSearchHit> searchProductionOrders(String query, Pageable limit) {
        return productionOrderRepository.searchByOrderNo(query, limit).stream()
                .map(order -> hit("PRODUCTION", getMessage("search.type.production"), "factory", order.getOrderNo(),
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
                    return hit("CUT_ORDER", getMessage("search.type.cut_order"), "scissors", order.getCutOrderNo(),
                            projectName, url, query);
                })
                .toList();
    }

    private List<GlobalSearchHit> searchSalesOrders(String query, Pageable limit) {
        return salesOrderRepository.searchByOrderNo(query, limit).stream()
                .map(order -> hit("SALES", getMessage("search.type.sales"), "shopping-cart", order.getOrderNo(),
                        order.getCustomer() != null ? order.getCustomer().getCompanyName() : null,
                        "/sales/" + order.getId(), query))
                .toList();
    }

    private List<GlobalSearchHit> searchPurchaseOrders(String query, Pageable limit) {
        return purchaseOrderRepository.searchByPoNumber(query, limit).stream()
                .map(order -> hit("PROCUREMENT", getMessage("search.type.procurement"), "truck", order.getPoNumber(),
                        order.getSupplier() != null ? order.getSupplier().getCompanyName() : null,
                        "/procurement/" + order.getId(), query))
                .toList();
    }

    private List<GlobalSearchHit> searchUsers(String query, Pageable limit) {
        return userRepository.searchActiveUsers(query, null, null, limit).getContent().stream()
                .map(user -> hit("USER", getMessage("search.type.user"), "users", user.getFullName(),
                        Strings.joinDistinct(" · ", user.getUsername(), user.getEmail()),
                        "/admin/users/" + user.getId() + "/edit", query))
                .toList();
    }

    private List<GlobalSearchHit> searchCustomers(String query, Pageable limit) {
        return customerRepository.searchByCodeOrName(query, limit).stream()
                .map(customer -> hit("CUSTOMER", getMessage("search.type.customer"), "contact",
                        Strings.joinDistinct(" · ", customer.getCustomerCode(), customer.getCompanyName()),
                        customer.getContactPerson(),
                        "/sales", query))
                .toList();
    }

    private List<GlobalSearchHit> searchSuppliers(String query, Pageable limit) {
        return supplierRepository.searchByCodeOrName(query, limit).stream()
                .map(supplier -> hit("SUPPLIER", getMessage("search.type.supplier"), "warehouse",
                        Strings.joinDistinct(" · ", supplier.getSupplierCode(), supplier.getCompanyName()),
                        supplier.getContactPerson(),
                        "/procurement", query))
                .toList();
    }

    private List<GlobalSearchHit> searchCostCenters(String query, Pageable limit) {
        return costCenterRepository.searchByCodeOrName(query, limit).stream()
                .map(center -> hit("COST_CENTER", getMessage("search.type.cost_center"), "calculator",
                        Strings.joinDistinct(" · ", center.getCode(), center.getName()),
                        center.getDescription(),
                        "/costs", query))
                .toList();
    }

    private List<GlobalSearchHit> searchCutItems(String query, Pageable limit) {
        return cutItemRepository.searchByItemCode(query, limit).stream()
                .map(item -> hit("CUT_ITEM", getMessage("search.type.cut_item"), "square", item.getItemCode(),
                        item.getTargetLocation(),
                        "/genealogy?code=" + Urls.encode(item.getItemCode()), query))
                .toList();
    }

    private List<GlobalSearchHit> searchQuarries(String query, Pageable limit) {
        return quarryRepository.searchByCodeOrName(query, limit).stream()
                .map(quarry -> hit("QUARRY", getMessage("search.type.quarry"), "mountain",
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
