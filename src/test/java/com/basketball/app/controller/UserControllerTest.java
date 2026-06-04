package com.basketball.app.controller;

import com.basketball.app.dto.UserCreateRequest;
import com.basketball.app.dto.UserUpdateRequest;
import com.basketball.app.model.User;
import com.basketball.app.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private com.basketball.app.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.basketball.app.security.CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Map<String, Object> userMap;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setEmail("john@test.com");
        testUser.setRole(User.Role.PLAYER);
        testUser.setIsActive(true);

        userMap = new HashMap<>();
        userMap.put("id", 1L);
        userMap.put("name", "John");
        userMap.put("surname", "Doe");
        userMap.put("displayName", "John Doe");
        userMap.put("email", "john@test.com");
        userMap.put("role", "PLAYER");

        User admin = new User();
        admin.setId(100L);
        admin.setRole(User.Role.ADMIN);
        when(userService.getCurrentUser()).thenReturn(admin);
    }

    @Test
    void testCreateUser_Success() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Jane");
        request.setSurname("Doe");
        request.setEmail("jane@test.com");
        request.setRole(User.Role.PLAYER);

        Map<String, Object> response = new HashMap<>();
        response.put("user", userMap);
        response.put("generatedPassword", "temp123");

        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.id").value(1L))
                .andExpect(jsonPath("$.generatedPassword").exists());

        verify(userService).createUser(any(UserCreateRequest.class));
    }

    @Test
    void testGetAllUsers_Success() throws Exception {
        List<User> users = Arrays.asList(testUser);
        when(userService.findAll()).thenReturn(users);
        when(userService.toUserMap(any(User.class))).thenReturn(userMap);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("John"));

        verify(userService).findAll();
    }

    @Test
    void testGetAllUsers_WithSearch() throws Exception {
        List<User> users = Arrays.asList(testUser);
        when(userService.searchUsers("john")).thenReturn(users);
        when(userService.toUserMap(any(User.class))).thenReturn(userMap);

        mockMvc.perform(get("/api/users")
                        .param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John"));

        verify(userService).searchUsers("john");
    }

    @Test
    void testGetAllUsers_WithRoleFilter() throws Exception {
        List<User> users = Arrays.asList(testUser);
        when(userService.findAll()).thenReturn(users);
        when(userService.toUserMap(any(User.class))).thenReturn(userMap);

        mockMvc.perform(get("/api/users")
                        .param("role", "PLAYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("PLAYER"));

        verify(userService).findAll();
    }

    @Test
    void testGetUserById_Success() throws Exception {
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(userService.toUserMap(any(User.class))).thenReturn(userMap);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John"));

        verify(userService).findById(1L);
    }

    @Test
    void testGetUserById_NotFound() throws Exception {
        when(userService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(userService).findById(999L);
    }

    @Test
    void testUpdateUser_Success() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Updated Name");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated Name");

        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(updatedUser);
        when(userService.toUserMap(any(User.class))).thenReturn(userMap);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(userService).updateUser(eq(1L), any(UserUpdateRequest.class));
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService).deleteUser(1L);
    }

    @Test
    void testGetAllUsers_Paginated_ReturnsPageWithStats() throws Exception {
        User player = new User();
        player.setId(2L);
        player.setName("Player");
        player.setSurname("Two");
        player.setRole(User.Role.PLAYER);
        player.setIsActive(true);

        when(userService.findAll()).thenReturn(List.of(testUser, player));
        when(userService.getPrimaryCategoryNameForSort(any())).thenReturn("U13");
        when(userService.toUserMap(any())).thenReturn(userMap);

        mockMvc.perform(get("/api/users").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.stats.activePlayers").value(2));
    }

    @Test
    void testGetAllUsers_WithCategoryFilter() throws Exception {
        when(userService.findAll()).thenReturn(List.of(testUser));
        when(userService.findByCategory(1L)).thenReturn(List.of(testUser));
        when(userService.getPrimaryCategoryNameForSort(any())).thenReturn("U13");
        when(userService.toUserMap(any())).thenReturn(userMap);

        mockMvc.perform(get("/api/users").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testGetAllUsers_SummaryMode_ReturnsSummaryMaps() throws Exception {
        when(userService.findAll()).thenReturn(List.of(testUser));
        when(userService.getPrimaryCategoryNameForSort(any())).thenReturn("U13");
        when(userService.toUserSummaryMap(any())).thenReturn(Map.of("id", 1L, "name", "John", "surname", "Doe", "displayName", "John Doe"));

        mockMvc.perform(get("/api/users").param("summary", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John"));
    }

    @Test
    void testGetAgeStructure_Success() throws Exception {
        when(userService.getAgeStructure()).thenReturn(List.of(Map.of("group", "U15", "count", 12)));

        mockMvc.perform(get("/api/users/age-structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].group").value("U15"));
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(userService.toUserMap(testUser)).thenReturn(userMap);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@test.com"));
    }

    @Test
    void testSearchUsers_Success() throws Exception {
        when(userService.searchUsers("john")).thenReturn(List.of(testUser));
        when(userService.toUserMap(testUser)).thenReturn(userMap);

        mockMvc.perform(get("/api/users/search").param("q", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John"));
    }

    @Test
    void testAssignToCategory_Success() throws Exception {
        when(userService.assignToCategory(1L, 2L)).thenReturn(testUser);
        when(userService.toUserMap(testUser)).thenReturn(userMap);

        mockMvc.perform(post("/api/users/1/assign-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testAssignToCategory_MissingCategoryId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users/1/assign-category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("categoryId is required"));
    }

    @Test
    void testChangePassword_AsAdmin_Success() throws Exception {
        when(userService.findById(2L)).thenReturn(Optional.of(testUser));
        doNothing().when(userService).changePassword(eq(2L), any());

        mockMvc.perform(post("/api/users/2/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"old","newPassword":"newpass1","confirmPassword":"newpass1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    void testChangePassword_NotOwnUser_AsNonAdmin_ReturnsForbidden() throws Exception {
        User trainer = new User();
        trainer.setId(50L);
        trainer.setRole(User.Role.TRAINER);
        when(userService.getCurrentUser()).thenReturn(trainer);
        when(userService.findById(2L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/users/2/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"old","newPassword":"newpass1","confirmPassword":"newpass1"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You can only change your own password"));
    }

    @Test
    void testCreateUser_ServiceError_ReturnsBadRequest() throws Exception {
        when(userService.createUser(any())).thenThrow(new RuntimeException("email exists"));

        UserCreateRequest request = new UserCreateRequest();
        request.setName("Jane");
        request.setSurname("Doe");
        request.setEmail("jane@test.com");
        request.setRole(User.Role.PLAYER);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("email exists"));
    }
}
