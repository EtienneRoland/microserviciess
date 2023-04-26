package com.orderservice.service;


import com.orderservice.dto.InventoryResponse;
import com.orderservice.dto.OrderLineItemsDto;
import com.orderservice.dto.OrderRequest;

import com.orderservice.model.Order;
import com.orderservice.model.OrderLineItems;
import com.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    private final Tracer tracer;
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

     /* List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);
        orderRepository.save(order);
    }*/
        List<OrderLineItemsDto> orderLineItemsDtoList = orderRequest.getOrderLineItemsDtoList();
        List<OrderLineItems> orderLineItems = new ArrayList<>();
        orderRequest.getOrderLineItemsDtoList().forEach(orderLineItemsDto -> {
            OrderLineItems lineItems = mapToDto(orderLineItemsDto);
            orderLineItems.add(lineItems);
        });
        order.setOrderLineItemsList(orderLineItems);

       /* List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();*/
        List<String> skuCodes = new ArrayList<>();
        List<OrderLineItems> orderLineItemsList = order.getOrderLineItemsList();

        for (OrderLineItems orderLineItem : orderLineItemsList) {
            skuCodes.add(orderLineItem.getSkuCode());
        }

        //call InventoryService, and place order if product is in stock
        Span inventoryServiceLookup = tracer.currentSpan().name("InventoryServiceLookup");

        try(Tracer.SpanInScope spanInScope =  tracer.withSpan(inventoryServiceLookup.start())){
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();
        /*boolean allProductsInStock = Arrays.stream(inventoryResponseArray)
                .allMatch(InventoryResponse::isInStock);     */
            boolean allProductsInStock = true;
            for (InventoryResponse response : inventoryResponseArray) {
                if (!response.isInStock()) {
                    allProductsInStock = false;
                    break;
                }
            }

            if(allProductsInStock){
                orderRepository.save(order);
                return "Order place succesfully!";
            }else {
                throw new IllegalArgumentException("product is not in stock, please try again later");
            }
        }finally{
            inventoryServiceLookup.end();
        }

    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        return orderLineItems;
    }



}
