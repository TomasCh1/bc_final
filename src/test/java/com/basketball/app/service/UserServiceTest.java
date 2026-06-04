package com.basketball.app.service;

import com.basketball.app.dto.PasswordChangeRequest;
import com.basketball.app.dto.UserCreateRequest;
import com.basketball.app.dto.UserUpdateRequest;
import com.basketball.app.model.Category;
import com.basketball.app.model.User;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John");
        testUser.setSurname("Doe");
        testUser.setEmail("john@test.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(User.Role.PLAYER);
        testUser.setIsActive(true);
        testUser.setMustChangePassword(false);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("U13");
        testCategory.setSeason("2024-2025");
    }

    @Test
    void testCreateUser_Success() {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Jane");
        request.setSurname("Doe");
        request.setEmail("jane@test.com");
        request.setRole(User.Role.PLAYER);
        request.setIsActive(true);
        request.setMustChangePassword(true);

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setName("Jane");
        savedUser.setSurname("Doe");
        savedUser.setEmail("jane@test.com");
        savedUser.setRole(User.Role.PLAYER);
        savedUser.setPassword("encodedPassword");

        when(userRepository.existsByEmailAndDeletedFalse("jane@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Map<String, Object> result = userService.createUser(request);

        assertNotNull(result);
        assertNotNull(result.get("user"));
        assertNotNull(result.get("generatedPassword"));
        verify(userRepository).existsByEmailAndDeletedFalse("jane@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUser_WithCategory() {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Jane");
        request.setSurname("Doe");
        request.setEmail("jane@test.com");
        request.setRole(User.Role.PLAYER);
        request.setCategoryIds(List.of(1L));

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setName("Jane");
        savedUser.setSurname("Doe");
        savedUser.setEmail("jane@test.com");
        savedUser.setRole(User.Role.PLAYER);

        when(userRepository.existsByEmailAndDeletedFalse("jane@test.com")).thenReturn(false);
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Map<String, Object> result = userService.createUser(request);

        assertNotNull(result);
        verify(categoryService).findById(1L);
    }

    @Test
    void testCreateUser_DuplicateEmail() {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail("john@test.com");

        when(userRepository.existsByEmailAndDeletedFalse("john@test.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_CategoryNotFound() {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail("jane@test.com");
        request.setCategoryIds(List.of(999L));

        when(userRepository.existsByEmailAndDeletedFalse("jane@test.com")).thenReturn(false);
        when(categoryService.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.createUser(request));
    }

    @Test
    void testUpdateUser_Success() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Updated Name");
        request.setEmail("updated@test.com");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("updated@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndDeletedFalse("updated@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser(1L, request);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUser_NotFound() {
        UserUpdateRequest request = new UserUpdateRequest();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.updateUser(999L, request));
    }

    @Test
    void testUpdateUser_DuplicateEmail() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("existing@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmailAndDeletedFalse("existing@test.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.updateUser(1L, request));
    }

    @Test
    void testFindById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userRepository).findById(1L);
    }

    @Test
    void testFindById_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindById_ReturnsEmptyWhenUserDeleted() {
        testUser.setDeleted(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAllWithCategories()).thenReturn(users);

        List<User> result = userService.findAll();

        assertEquals(1, result.size());
        verify(userRepository).findAllWithCategories();
    }

    @Test
    void testSearchUsers() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.searchByNameOrEmail("john")).thenReturn(users);

        List<User> result = userService.searchUsers("john");

        assertEquals(1, result.size());
        verify(userRepository).searchByNameOrEmail("john");
    }

    @Test
    void testSearchUsers_EmptySearch() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAllWithCategories()).thenReturn(users);

        List<User> result = userService.searchUsers("");

        assertEquals(1, result.size());
        verify(userRepository).findAllWithCategories();
    }

    @Test
    void testFindByRole() {
        List<User> players = Arrays.asList(testUser);
        when(userRepository.findByRole(User.Role.PLAYER)).thenReturn(players);

        List<User> result = userService.findByRole(User.Role.PLAYER);

        assertEquals(1, result.size());
        verify(userRepository).findByRole(User.Role.PLAYER);
    }

    @Test
    void testFindByCategory() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findByCategoryInCategoriesOrCoached(1L)).thenReturn(users);

        List<User> result = userService.findByCategory(1L);

        assertEquals(1, result.size());
        verify(userRepository).findByCategoryInCategoriesOrCoached(1L);
    }

    @Test
    void testFindByRoleAndCategory() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findByRoleAndCategoryInCategoriesOrCoached(User.Role.PLAYER, 1L))
                .thenReturn(users);

        List<User> result = userService.findByRoleAndCategory(User.Role.PLAYER, 1L);

        assertEquals(1, result.size());
        verify(userRepository).findByRoleAndCategoryInCategoriesOrCoached(User.Role.PLAYER, 1L);
    }

    @Test
    void testDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.deleteUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        assertTrue(Boolean.TRUE.equals(testUser.getDeleted()));
        assertFalse(testUser.getIsActive());
        assertTrue(testUser.getEmail().startsWith("__deleted_") && testUser.getEmail().endsWith("@deleted.local"));
    }

    @Test
    void testDeleteUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.deleteUser(999L));
    }

    @Test
    void testDeleteUser_AlreadyDeleted() {
        testUser.setDeleted(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_Success() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        testUser.setMustChangePassword(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.changePassword(1L, request);

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testChangePassword_FirstLogin() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        testUser.setMustChangePassword(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.changePassword(1L, request);

        verify(userRepository).findById(1L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    void testChangePassword_WrongCurrentPassword() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        testUser.setMustChangePassword(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.changePassword(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_PasswordMismatch() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("differentPassword");

        testUser.setMustChangePassword(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.changePassword(1L, request));
    }

    @Test
    void testChangePassword_UserNotFound() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.changePassword(999L, request));
    }

    @Test
    void testHardDeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        userService.hardDeleteUser(1L);

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void testHardDeleteUser_NotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> userService.hardDeleteUser(999L));
        verify(userRepository, never()).deleteById(anyLong());
    }
}
