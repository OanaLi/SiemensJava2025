package com.siemens.internship.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        item1 = new Item(1L, "Item 1", "Description 1", "NEW", "item1@test.com");
        item2 = new Item(2L, "Item 2", "Description 2", "OLD", "item2@test.com");
    }

    @Test
    void shouldReturnAllItems() throws Exception {
        BDDMockito.given(itemService.findAll()).willReturn(Arrays.asList(item1, item2));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].name", is("Item 1")));
    }

    @Test
    void shouldReturnItemByIdIfExists() throws Exception {
        BDDMockito.given(itemService.findById(1L)).willReturn(Optional.of(item1));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Item 1")));
    }

    @Test
    void shouldReturnNotFoundForNonExistingItem() throws Exception {
        BDDMockito.given(itemService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/items/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateItemWhenValid() throws Exception {
        Item input = new Item(null, "New Item", "Some desc", "NEW", "valid@email.com");
        Item saved = new Item(3L, "New Item", "Some desc", "NEW", "valid@email.com");

        BDDMockito.given(itemService.save(any(Item.class))).willReturn(saved);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.email", is("valid@email.com")));
    }

    @Test
    void shouldRejectInvalidEmailOnCreate() throws Exception {
        Item invalid = new Item(null, "Invalid", "Desc", "NEW", "bad-email");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0]", containsString("email")));
    }

    @Test
    void shouldUpdateItemFieldsWhenFound() throws Exception {
        Item updates = new Item(null, "Updated", null, "UPDATED", "updated@email.com");
        Item updatedItem = new Item(1L, "Updated", "Description 1", "UPDATED", "updated@email.com");

        BDDMockito.given(itemService.findById(1L)).willReturn(Optional.of(item1));
        BDDMockito.given(itemService.save(any(Item.class))).willReturn(updatedItem);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated")))
                .andExpect(jsonPath("$.status", is("UPDATED")));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistingItem() throws Exception {
        BDDMockito.given(itemService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(put("/api/items/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteItemWhenExists() throws Exception {
        BDDMockito.given(itemService.existsById(1L)).willReturn(true);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundOnDeleteIfItemNotFound() throws Exception {
        BDDMockito.given(itemService.existsById(99L)).willReturn(false);

        mockMvc.perform(delete("/api/items/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnProcessedItems() throws Exception {
        item1.setStatus("PROCESSED");
        item2.setStatus("PROCESSED");

        BDDMockito.given(itemService.processItemsAsync())
                .willReturn(CompletableFuture.completedFuture(Arrays.asList(item1, item2)));

        mockMvc.perform(get("/api/items/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].status", is("PROCESSED")));
    }
}
