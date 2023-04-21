package com.inventoryservice.service;


import com.inventoryservice.dto.InventoryResponse;
import com.inventoryservice.model.Inventory;
import com.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
/*
    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCode){
        return inventoryRepository.findBySkuCodeIn(skuCode).stream()
                .map(inventory ->
                    InventoryResponse.builder()
                            .skuCode(inventory.getSkuCode())
                            .isInStock(inventory.getQuantity() > 0)
                            .build()
                ).toList();

    }*/
    @Transactional(readOnly = true)
    @SneakyThrows
    public List<InventoryResponse> isInStock(List<String> skuCode){
        List<Inventory> inventoryList = inventoryRepository.findBySkuCodeIn(skuCode);
        List<InventoryResponse> inventoryResponses = new ArrayList<>();
        log.info("Wait Started");
        Thread.sleep(1000);
        log.info("Wait Ended");
        for (Inventory inventory : inventoryList) {
            boolean inStock = inventory.getQuantity() > 0;
            InventoryResponse response = InventoryResponse.builder()
                    .skuCode(inventory.getSkuCode())
                    .isInStock(inStock)
                    .build();
            inventoryResponses.add(response);
        }
        return inventoryResponses;
    }
}
