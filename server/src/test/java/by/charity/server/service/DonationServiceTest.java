package by.charity.server.service;

import by.charity.server.repository.DonationRepository;
import by.charity.server.repository.FundRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.*;
import by.charity.shared.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты DonationService")
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private FundRepository fundRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    private DonationService donationService;

    private User managerUser;
    private User guestUser;
    private CharityFund testFund;
    private Donation validDonation;

    @BeforeEach
    void setUp() {
        donationService = new DonationService();
        // Внедряем моки вручную через рефлексию или через конструктор
        injectMocks();

        managerUser = new User();
        managerUser.setId(1L);
        managerUser.setUsername("manager");
        managerUser.setRole(Role.MANAGER);
        managerUser.setActive(true);

        guestUser = new User();
        guestUser.setId(2L);
        guestUser.setUsername("guest");
        guestUser.setRole(Role.GUEST);
        guestUser.setActive(true);

        testFund = new CharityFund();
        testFund.setId(1L);
        testFund.setName("Тестовый фонд");
        testFund.setTotalReceived(BigDecimal.ZERO);
        testFund.setTotalSpent(BigDecimal.ZERO);
        testFund.setActive(true);

        validDonation = new Donation();
        validDonation.setFundId(1L);
        validDonation.setDonorName("Иван Иванов");
        validDonation.setAmount(new BigDecimal("100.00"));
        validDonation.setPaymentMethod(Donation.PaymentMethod.ERIP);
    }

    private void injectMocks() {
        try {
            var donationRepoField = DonationService.class
                    .getDeclaredField("donationRepository");
            donationRepoField.setAccessible(true);
            donationRepoField.set(donationService, donationRepository);

            var fundRepoField = DonationService.class
                    .getDeclaredField("fundRepository");
            fundRepoField.setAccessible(true);
            fundRepoField.set(donationService, fundRepository);

            var projectServiceField = DonationService.class
                    .getDeclaredField("projectService");
            projectServiceField.setAccessible(true);
            projectServiceField.set(donationService, projectService);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка внедрения моков", e);
        }
    }

    @Test
    @DisplayName("Успешная регистрация пожертвования обновляет баланс фонда")
    void registerDonation_valid_updatesFundBalance() {
        when(userService.getUserByToken("token")).thenReturn(managerUser);
        when(donationRepository.save(any())).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });
        when(donationRepository.sumByFundId(1L))
                .thenReturn(new BigDecimal("100.00"));
        when(fundRepository.findById(1L)).thenReturn(Optional.of(testFund));

        Donation result = donationService.registerDonation(
                "token", validDonation, userService);

        assertNotNull(result.getId());
        verify(fundRepository, times(1))
                .updateTotals(eq(1L), any(), any());
    }

    @Test
    @DisplayName("Пожертвование с нулевой суммой запрещено")
    void registerDonation_zeroAmount_throwsValidationException() {
        when(userService.getUserByToken("token")).thenReturn(managerUser);

        validDonation.setAmount(BigDecimal.ZERO);

        assertThrows(CharityException.ValidationException.class,
                () -> donationService.registerDonation(
                        "token", validDonation, userService));
    }

    @Test
    @DisplayName("Пожертвование с отрицательной суммой запрещено")
    void registerDonation_negativeAmount_throwsValidationException() {
        when(userService.getUserByToken("token")).thenReturn(managerUser);

        validDonation.setAmount(new BigDecimal("-50.00"));

        assertThrows(CharityException.ValidationException.class,
                () -> donationService.registerDonation(
                        "token", validDonation, userService));
    }

    @Test
    @DisplayName("Пожертвование без имени донора запрещено")
    void registerDonation_emptyDonorName_throwsValidationException() {
        when(userService.getUserByToken("token")).thenReturn(managerUser);

        validDonation.setDonorName("");

        assertThrows(CharityException.ValidationException.class,
                () -> donationService.registerDonation(
                        "token", validDonation, userService));
    }

    @Test
    @DisplayName("Пожертвование без указания фонда запрещено")
    void registerDonation_nullFundId_throwsValidationException() {
        when(userService.getUserByToken("token")).thenReturn(managerUser);

        validDonation.setFundId(null);

        assertThrows(CharityException.ValidationException.class,
                () -> donationService.registerDonation(
                        "token", validDonation, userService));
    }

    @Test
    @DisplayName("Гость может зарегистрировать пожертвование")
    void registerDonation_byGuest_allowed() {
        when(userService.getUserByToken("guesttoken")).thenReturn(guestUser);
        when(donationRepository.save(any())).thenAnswer(inv -> {
            Donation d = inv.getArgument(0);
            d.setId(2L);
            return d;
        });
        when(donationRepository.sumByFundId(1L))
                .thenReturn(new BigDecimal("100.00"));
        when(fundRepository.findById(1L)).thenReturn(Optional.of(testFund));

        Donation result = donationService.registerDonation(
                "guesttoken", validDonation, userService);

        assertNotNull(result.getId());
    }

    @Test
    @DisplayName("Статистика корректно считает сумму и количество")
    void getDonationStatistics_nonEmpty_correctCalculation() {
        when(userService.getUserByToken("token")).thenReturn(managerUser);

        Donation d1 = new Donation();
        d1.setAmount(new BigDecimal("100.00"));
        d1.setFundId(1L);

        Donation d2 = new Donation();
        d2.setAmount(new BigDecimal("250.50"));
        d2.setFundId(1L);

        Donation d3 = new Donation();
        d3.setAmount(new BigDecimal("50.00"));
        d3.setFundId(1L);

        when(donationRepository.findByFundId(1L))
                .thenReturn(List.of(d1, d2, d3));

        DonationService.DonationStatistics stats =
                donationService.getDonationStatistics("token", 1L, userService);

        assertEquals(3, stats.count());
        assertEquals(new BigDecimal("400.50"), stats.total());
        assertEquals(new BigDecimal("250.50"), stats.max());
    }

    @Test
    @DisplayName("Статистика для пустого списка пожертвований возвращает нули")
    void getDonationStatistics_empty_returnsZeros() {
        when(userService.getUserByToken("token")).thenReturn(managerUser);
        when(donationRepository.findByFundId(99L)).thenReturn(List.of());

        DonationService.DonationStatistics stats =
                donationService.getDonationStatistics("token", 99L, userService);

        assertEquals(0, stats.count());
        assertEquals(BigDecimal.ZERO, stats.total());
    }
}