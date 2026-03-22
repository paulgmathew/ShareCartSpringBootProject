package com.sharecart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharecart.entity.User;
import com.sharecart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ShareCartApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        savedUser = userRepository.save(new User("alice", "alice@example.com"));
    }

    @Test
    void createList_returns201WithList() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "Weekly Groceries",
                "ownerId", savedUser.getId()
        );

        mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Weekly Groceries"));
    }

    @Test
    void createList_withUnknownOwner_returns404() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "My List",
                "ownerId", 9999
        );

        mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getList_returnsListWithItems() throws Exception {
        // Create a list first
        Map<String, Object> createBody = Map.of(
                "name", "Party Supplies",
                "ownerId", savedUser.getId()
        );

        String response = mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long listId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/lists/" + listId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listId))
                .andExpect(jsonPath("$.name").value("Party Supplies"));
    }

    @Test
    void getList_withUnknownId_returns404() throws Exception {
        mockMvc.perform(get("/lists/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void inviteUser_returns201WithMember() throws Exception {
        User invitee = userRepository.save(new User("bob", "bob@example.com"));

        Map<String, Object> createBody = Map.of(
                "name", "Shared List",
                "ownerId", savedUser.getId()
        );

        String listResponse = mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long listId = objectMapper.readTree(listResponse).get("id").asLong();

        Map<String, Object> inviteBody = Map.of("userId", invitee.getId());

        mockMvc.perform(post("/lists/" + listId + "/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void inviteUser_duplicate_returns409() throws Exception {
        User invitee = userRepository.save(new User("charlie", "charlie@example.com"));

        Map<String, Object> createBody = Map.of(
                "name", "Another List",
                "ownerId", savedUser.getId()
        );

        String listResponse = mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long listId = objectMapper.readTree(listResponse).get("id").asLong();
        Map<String, Object> inviteBody = Map.of("userId", invitee.getId());

        // First invite
        mockMvc.perform(post("/lists/" + listId + "/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteBody)))
                .andExpect(status().isCreated());

        // Duplicate invite
        mockMvc.perform(post("/lists/" + listId + "/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inviteBody)))
                .andExpect(status().isConflict());
    }

    @Test
    void addItem_returns201WithItem() throws Exception {
        Map<String, Object> createBody = Map.of(
                "name", "Grocery List",
                "ownerId", savedUser.getId()
        );

        String listResponse = mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long listId = objectMapper.readTree(listResponse).get("id").asLong();

        Map<String, Object> itemBody = Map.of("name", "Milk", "quantity", 2);

        mockMvc.perform(post("/lists/" + listId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Milk"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.checked").value(false));
    }

    @Test
    void updateItem_returnsUpdatedItem() throws Exception {
        Map<String, Object> createBody = Map.of(
                "name", "Update Test List",
                "ownerId", savedUser.getId()
        );

        String listResponse = mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long listId = objectMapper.readTree(listResponse).get("id").asLong();

        Map<String, Object> itemBody = Map.of("name", "Eggs", "quantity", 12);
        String itemResponse = mockMvc.perform(post("/lists/" + listId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(itemResponse).get("id").asLong();

        Map<String, Object> updateBody = Map.of("checked", true, "quantity", 6);

        mockMvc.perform(put("/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true))
                .andExpect(jsonPath("$.quantity").value(6));
    }

    @Test
    void deleteItem_returns204() throws Exception {
        Map<String, Object> createBody = Map.of(
                "name", "Delete Test List",
                "ownerId", savedUser.getId()
        );

        String listResponse = mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long listId = objectMapper.readTree(listResponse).get("id").asLong();

        Map<String, Object> itemBody = Map.of("name", "Bread", "quantity", 1);
        String itemResponse = mockMvc.perform(post("/lists/" + listId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(itemResponse).get("id").asLong();

        mockMvc.perform(delete("/items/" + itemId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/lists/" + listId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
