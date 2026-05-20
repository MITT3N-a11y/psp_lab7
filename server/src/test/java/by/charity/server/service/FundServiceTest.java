package by.charity.server.service;

import by.charity.server.repository.FundRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.CharityFund;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Тесты FundService")
class FundServiceTest {

    @Mock
    private FundRepository fundRepository;

    @Mock
    private UserService userService;

    private FundService fundService;

    private User adminUser;
    private User guestUser;
    private CharityFund testFund;

    @BeforeEach
    void setUp() throws Exception {
        fundService = new FundService();
        var field = FundService.class.getDeclaredField("fundRepository");
        field.setAccessible(true);
        field.set(fundService, fundRepository);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);

        guestUser = new User();
        guestUser.setId(2L);
        guestUser.setRole(Role.GUEST);
        guestUser.setActive(true);

        testFund = new CharityFund();
        testFund.setId(1L);
        testFund.setName("Помощь детям");
        testFund.setTotalReceived(new BigDecimal("5000.00"));
        testFund.setTotalSpent(new BigDecimal("2000.00"));
        testFund.setActive(true);
    }

    @Test
    @DisplayName("Любой авторизованный пользователь получает список фондов")
    void getAllFunds_anyUser_returnsList() {
        when(userService.getUserByToken("token")).thenReturn(guestUser);
        when(fundRepository.findAll()).thenReturn(List.of(testFund));

        List<CharityFund> result = fundService.getAllFunds("token", userService);

        assertEquals(1, result.size());
        assertEquals("Помощь детям", result.get(0).getName());
    }

    @Test
    @DisplayName("Администратор создаёт фонд успешно")
    void createFund_byAdmin_savedSuccessfully() {
        doNothing().when(userService)
                .requireRole("token", Role.ADMIN, Role.MANAGER);
        when(fundRepository.save(any())).thenAnswer(inv -> {
            CharityFund f = inv.getArgument(0);
            f.setId(1L);
            return f;
        });

        CharityFund newFund = new CharityFund();
        newFund.setName("Новый фонд");
        newFund.setDescription("Описание");

        CharityFund result = fundService.createFund("token", newFund, userService);

        assertNotNull(result.getId());
        verify(fundRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Создание фонда без названия запрещено")
    void createFund_emptyName_throwsValidationException() {
        doNothing().when(userService)
                .requireRole("token", Role.ADMIN, Role.MANAGER);

        CharityFund invalidFund = new CharityFund();
        invalidFund.setName("");

        assertThrows(CharityException.ValidationException.class,
                () -> fundService.createFund("token", invalidFund, userService));
    }

    @Test
    @DisplayName("Создание фонда с null названием запрещено")
    void createFund_nullName_throwsValidationException() {
        doNothing().when(userService)
                .requireRole("token", Role.ADMIN, Role.MANAGER);

        CharityFund invalidFund = new CharityFund();
        invalidFund.setName(null);

        assertThrows(CharityException.ValidationException.class,
                () -> fundService.createFund("token", invalidFund, userService));
    }

    @Test
    @DisplayName("Баланс фонда рассчитывается корректно")
    void charityFund_getBalance_correctCalculation() {
        assertEquals(new BigDecimal("3000.00"), testFund.getBalance());
    }

    @Test
    @DisplayName("Запрос несуществующего фонда выбрасывает NotFoundException")
    void getFundById_notFound_throwsNotFoundException() {
        when(userService.getUserByToken("token")).thenReturn(adminUser);
        when(fundRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CharityException.NotFoundException.class,
                () -> fundService.getFundById("token", 999L, userService));
    }

    @Test
    @DisplayName("Деактивация фонда меняет его статус")
    void deactivateFund_existing_setsInactive() {
        doNothing().when(userService).requireRole("token", Role.ADMIN);
        when(fundRepository.findById(1L)).thenReturn(Optional.of(testFund));

        fundService.deactivateFund("token", 1L, userService);

        assertFalse(testFund.isActive());
        verify(fundRepository, times(1)).update(testFund);
    }
}