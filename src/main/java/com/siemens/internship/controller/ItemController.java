package com.siemens.internship.controller;

import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return new ResponseEntity<>(itemService.findAll(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {

       if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors().stream()
                    .map(error -> error.getField() + " Error")
                    .collect(Collectors.toList());
             return ResponseEntity.badRequest().body(errors); // Returns error list
        }
        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> new ResponseEntity<>(item, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    // @Transactional might be a more efficient choice
    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id,@Valid @RequestBody Item item) {

        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); //ERROR 404 -> item does not exist
        }

        Item updatedItem = existingItem.get();

        if(item.getName() != null){
            updatedItem.setName(item.getName());
        }
        if(item.getDescription() != null){
            updatedItem.setDescription(item.getDescription());
        }
        if(item.getStatus() != null){
            updatedItem.setStatus(item.getStatus());
        }

        return new ResponseEntity<>(itemService.save(updatedItem), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (!itemService.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        itemService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); //NOT CONFLICT
    }

    @GetMapping("/process")
    public ResponseEntity<List<Item>> processItems() throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(itemService.processItemsAsync().get(), HttpStatus.OK);
    }
}
