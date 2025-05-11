package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final Executor executor; //I used Executor for a lower coupling

    //private List<Item> processedItems = new ArrayList<>();
    private final AtomicInteger processedCount = new AtomicInteger(0);
    //Old: private int processedCount = 0;  //Mention: A variable needs to be atomic in order to be thread safe.

    @Autowired
    //added constructor for itemRepository and Executor dependency injection
    //@Qualifier("itemProcessingTaskExecutor") tells Spring to use the personalized Bean from Main class
    public ItemService(ItemRepository itemRepository, @Qualifier("itemTaskExecutor") Executor executor) {
        this.itemRepository = itemRepository;
        this.executor = executor;
    }

    public int getProcessedCount() {
        return processedCount.get();
    }

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public boolean existsById(Long id) { return itemRepository.existsById(id); }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     * <p>
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */


    @Async
    // Mention:the function needs to return CompletableFuture -> is a special type for asynchronous results
    // CompletableFuture is a promise for async functions that the caller will receive the data when it's ready.
    // We can't use a simple List as a return type, because @Async makes the function return instantly, before
    // the list can be populated.
    public CompletableFuture<List<Item>> processItemsAsync(){

        List<Long> itemIds = itemRepository.findAllIds();
        if (itemIds == null || itemIds.isEmpty()) {
            return null;
        }

        List<CompletableFuture<Item>> futureItems = new ArrayList<>();
        for (Long id : itemIds) {
            //Old: CompletableFuture.runAsync(() -> {
            //Mention: changed to supplyAsync() to return results
            CompletableFuture<Item> completableFutureItem = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);

                    Item item = itemRepository.findById(id).orElse(null);
                    if (item == null) {
                        System.out.println(id + " not found");
                        return null;
                    }


                    item.setStatus("PROCESSED");
                    itemRepository.save(item); //first save, then increment
                    processedCount.incrementAndGet();

                    return item;

                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }, executor);

            futureItems.add(completableFutureItem);
        }

        CompletableFuture<Void> allFutureItems = CompletableFuture.allOf(
                futureItems.toArray(new CompletableFuture[0])
        );

        return allFutureItems.thenApply(v ->
                futureItems.stream()
                        .map(futureItem -> {
                            try {
                                return futureItem.join();
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }


}

