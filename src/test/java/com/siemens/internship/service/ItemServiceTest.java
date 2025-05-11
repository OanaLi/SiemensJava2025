package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepositoryMock;

    @Mock
    private Executor executorMock;

    @InjectMocks
    private ItemService itemService;

    private Item item1, item2;

    @BeforeEach
    void setUp() {
        item1 = new Item(1L, "Item 1", "Description 1", "NEW", "item1@test.com");
        item2 = new Item(2L, "Item 2", "Description 2", "OLD", "iteml2@test.com");

    }

    @Test
    void findAll() {
        List<Item> expectedItems = Arrays.asList(item1, item2);
        given(itemRepositoryMock.findAll()).willReturn(expectedItems);

        List<Item> actualItems = itemService.findAll();

        assertNotNull(actualItems);
        assertEquals(2, actualItems.size());
        assertEquals(expectedItems, actualItems);
        verify(itemRepositoryMock).findAll();
    }

    @Test
    void findById() {
        Long existingId = 1L;
        given(itemRepositoryMock.findById(existingId)).willReturn(Optional.of(item1));

        Optional<Item> resultWhenFound = itemService.findById(existingId);
        assertTrue(resultWhenFound.isPresent());
        assertEquals(item1.getName(), resultWhenFound.get().getName());
        verify(itemRepositoryMock).findById(existingId);

        Long nonExistingId = 100L;
        given(itemRepositoryMock.findById(nonExistingId)).willReturn(Optional.empty());
        Optional<Item> resultWhenNotFound = itemService.findById(nonExistingId);
        assertFalse(resultWhenNotFound.isPresent());
        verify(itemRepositoryMock).findById(nonExistingId);
    }

    @Test
    void existsById() {
        Long existingId = 1L;
        given(itemRepositoryMock.existsById(existingId)).willReturn(true);
        boolean exists = itemService.existsById(existingId);
        assertTrue(exists);
        verify(itemRepositoryMock).existsById(existingId);

        Long nonExistingId = 100L;
        given(itemRepositoryMock.existsById(nonExistingId)).willReturn(false);
        boolean notExists = itemService.existsById(nonExistingId);
        assertFalse(notExists);
        verify(itemRepositoryMock).existsById(nonExistingId);
    }

    @Test
    void save() {
        Item itemToSave = new Item(null, "Item", "Desc Save", "NEW", "save@test.com");
        Item savedItem = new Item(5L, "Item", "Desc Save", "NEW", "save@test.com");
        given(itemRepositoryMock.save(any(Item.class))).willReturn(savedItem);

        Item result = itemService.save(itemToSave);
        assertNotNull(result);
        assertEquals(savedItem.getId(), result.getId());
        assertEquals(itemToSave.getName(), result.getName());
        verify(itemRepositoryMock).save(itemToSave);
    }

    @Test
    void deleteById() {
        Long id = 1L;
        itemService.deleteById(id);
        verify(itemRepositoryMock).deleteById(id);
    }

    @Test
    void processItemsAsync() throws Exception {

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(executorMock).execute(any(Runnable.class));

        List<Long> ids = Arrays.asList(1L, 2L);
        Item processedItem1 = new Item(1L, "Item 1", "Desc 1", "PROCESSED", "item1@example.com");
        Item processedItem2 = new Item(2L, "Item 2", "Desc 2", "PROCESSED", "item2@example.com");


        given(itemRepositoryMock.findAllIds()).willReturn(ids);
        given(itemRepositoryMock.findById(1L)).willReturn(Optional.of(item1));
        given(itemRepositoryMock.findById(2L)).willReturn(Optional.of(item2));

        given(itemRepositoryMock.save(item1)).willReturn(processedItem1);
        given(itemRepositoryMock.save(item2)).willReturn(processedItem2);;


        int initialProcessedCount = itemService.getProcessedCount();

        CompletableFuture<List<Item>> futureResult = itemService.processItemsAsync();
        List<Item> resultList = futureResult.get(100, TimeUnit.SECONDS);

        assertNotNull(resultList);
        assertEquals(2, resultList.size());
        assertTrue(resultList.stream().allMatch(item -> "PROCESSED".equals(item.getStatus())));
        assertEquals(initialProcessedCount + 2, itemService.getProcessedCount());

        verify(itemRepositoryMock).findAllIds();
        verify(itemRepositoryMock, times(2)).findById(anyLong());
        verify(itemRepositoryMock, times(2)).save(any(Item.class));
    }

}