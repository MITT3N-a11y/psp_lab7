package by.charity.server.service;

import by.charity.server.repository.UserRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;
import by.charity.shared.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Тесты UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    private User adminUser;
    private User guestUser;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserService();
        // Внедряем мок репозитория через рефлексию
        var field = UserService.class.getDeclaredField("userRepository");
        field.setAccessible(true);
        field.set(userService, userRepository);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setPasswordHash(PasswordUtil.hashPassword("admin"));
        adminUser.setFullName("Администратор");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);

        guestUser = new User();
        guestUser.setId(2L);
        guestUser.setUsername("guest");
        guestUser.setPasswordHash(PasswordUtil.hashPassword("password123"));
        guestUser.setFullName("Гость Системы");
        guestUser.setRole(Role.GUEST);
        guestUser.setActive(true);
    }

    // ===== Логин =====

    @Test
    @DisplayName("Успешный вход с верными данными возвращает токен")
    void login_correctCredentials_returnsToken() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));

        String token = userService.login("admin", "admin");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Неверный пароль выбрасывает AuthException")
    void login_wrongPassword_throwsAuthException() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));

        CharityException.AuthException ex = assertThrows(
                CharityException.AuthException.class,
                () -> userService.login("admin", "wrongpassword"));

        assertEquals("Неверный пароль", ex.getMessage());
    }

    @Test
    @DisplayName("Вход несуществующего пользователя выбрасывает AuthException")
    void login_unknownUser_throwsAuthException() {
        when(userRepository.findByUsername("nobody"))
                .thenReturn(Optional.empty());

        assertThrows(CharityException.AuthException.class,
                () -> userService.login("nobody", "anypassword"));
    }

    @Test
    @DisplayName("Вход деактивированного пользователя запрещён")
    void login_inactiveUser_throwsAuthException() {
        User inactiveUser = new User();
        inactiveUser.setId(3L);
        inactiveUser.setUsername("inactive");
        inactiveUser.setPasswordHash(PasswordUtil.hashPassword("pass123"));
        inactiveUser.setRole(Role.MANAGER);
        inactiveUser.setActive(false); // деактивирован

        when(userRepository.findByUsername("inactive"))
                .thenReturn(Optional.of(inactiveUser));

        CharityException.AuthException ex = assertThrows(
                CharityException.AuthException.class,
                () -> userService.login("inactive", "pass123"));

        assertEquals("Учётная запись деактивирована", ex.getMessage());
    }

    // ===== Токен =====

    @Test
    @DisplayName("Получение пользователя по валидному токену")
    void getUserByToken_validToken_returnsUser() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        String token = userService.login("admin", "admin");

        User result = userService.getUserByToken(token);

        assertEquals("admin", result.getUsername());
    }

    @Test
    @DisplayName("Невалидный токен выбрасывает AuthException")
    void getUserByToken_invalidToken_throwsAuthException() {
        assertThrows(CharityException.AuthException.class,
                () -> userService.getUserByToken("invalid-token-xyz"));
    }

    @Test
    @DisplayName("Null токен выбрасывает AuthException")
    void getUserByToken_nullToken_throwsAuthException() {
        assertThrows(CharityException.AuthException.class,
                () -> userService.getUserByToken(null));
    }

    // ===== Выход =====

    @Test
    @DisplayName("После logout токен становится недействительным")
    void logout_validToken_tokenInvalidated() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        String token = userService.login("admin", "admin");

        userService.logout(token);

        assertThrows(CharityException.AuthException.class,
                () -> userService.getUserByToken(token));
    }

    // ===== Роли =====

    @Test
    @DisplayName("requireRole не бросает исключение для верной роли")
    void requireRole_correctRole_noException() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        String token = userService.login("admin", "admin");

        assertDoesNotThrow(() -> userService.requireRole(token, Role.ADMIN));
    }

    @Test
    @DisplayName("requireRole бросает исключение для неверной роли")
    void requireRole_wrongRole_throwsAuthException() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        String token = userService.login("admin", "admin");

        assertThrows(CharityException.AuthException.class,
                () -> userService.requireRole(token, Role.GUEST));
    }

    // ===== Регистрация гостя =====

    @Test
    @DisplayName("Регистрация нового гостя выполняется успешно")
    void registerGuest_newUser_savedSuccessfully() {
        when(userRepository.findByUsername("newguest"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setId(10L);
                    return u;
                });

        User newUser = new User();
        newUser.setUsername("newguest");
        newUser.setFullName("Новый Гость");
        newUser.setPasswordHash(PasswordUtil.hashPassword("pass123"));

        User result = userService.registerGuest(newUser);

        assertEquals(Role.GUEST, result.getRole());
        assertTrue(result.isActive());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация с занятым логином выбрасывает ValidationException")
    void registerGuest_duplicateUsername_throwsValidationException() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));

        User duplicate = new User();
        duplicate.setUsername("admin");

        assertThrows(CharityException.ValidationException.class,
                () -> userService.registerGuest(duplicate));
    }

    // ===== Создание пользователя =====

    @Test
    @DisplayName("Администратор создаёт нового пользователя")
    void createUser_byAdmin_createdSuccessfully() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        String token = userService.login("admin", "admin");

        when(userRepository.findByUsername("newmanager"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setId(5L);
                    return u;
                });

        User newManager = new User();
        newManager.setUsername("newmanager");
        newManager.setFullName("Новый Менеджер");
        newManager.setRole(Role.MANAGER);

        User result = userService.createUser(token, newManager);

        assertNotNull(result.getId());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Гость не может создать пользователя")
    void createUser_byGuest_throwsAuthException() {
        when(userRepository.findByUsername("guest"))
                .thenReturn(Optional.of(guestUser));
        String token = userService.login("guest", "password123");

        User newUser = new User();
        newUser.setUsername("someone");

        assertThrows(CharityException.AuthException.class,
                () -> userService.createUser(token, newUser));
    }

    // ===== Деактивация =====

    @Test
    @DisplayName("Нельзя деактивировать самого себя")
    void deactivateUser_selfDeactivation_throwsValidationException() {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        String token = userService.login("admin", "admin");

        assertThrows(CharityException.ValidationException.class,
                () -> userService.deactivateUser(token, adminUser.getId()));
    }

    // ===== Смена пароля =====

    @Test
    @DisplayName("Неверный текущий пароль при смене запрещён")
    void changePassword_wrongOldPassword_throwsAuthException() {
        when(userRepository.findByUsername("guest"))
                .thenReturn(Optional.of(guestUser));
        String token = userService.login("guest", "password123");

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(guestUser));

        assertThrows(CharityException.AuthException.class,
                () -> userService.changePassword(
                        token, 2L, "wrongold", "newpass123"));
    }

    @Test
    @DisplayName("Слишком короткий новый пароль запрещён")
    void changePassword_shortNewPassword_throwsValidationException() {
        when(userRepository.findByUsername("guest"))
                .thenReturn(Optional.of(guestUser));
        String token = userService.login("guest", "password123");

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(guestUser));

        assertThrows(CharityException.ValidationException.class,
                () -> userService.changePassword(
                        token, 2L, "password123", "123"));
    }

    // ===== Хеширование =====

    @Test
    @DisplayName("Одинаковые пароли дают одинаковый хеш")
    void hashPassword_sameInput_sameHash() {
        String h1 = UserService.hashPassword("mypassword");
        String h2 = UserService.hashPassword("mypassword");
        assertEquals(h1, h2);
    }

    @Test
    @DisplayName("Разные пароли дают разные хеши")
    void hashPassword_differentInput_differentHash() {
        String h1 = UserService.hashPassword("password1");
        String h2 = UserService.hashPassword("password2");
        assertNotEquals(h1, h2);
    }
}