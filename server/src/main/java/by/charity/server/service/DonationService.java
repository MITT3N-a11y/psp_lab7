package by.charity.server.service;

import by.charity.server.repository.DonationRepository;
import by.charity.server.repository.FundRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Donation;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class DonationService {
    private final DonationRepository donationRepository =
            new DonationRepository();
    private final FundRepository fundRepository = new FundRepository();
    private final ProjectService projectService = new ProjectService();

    public List<Donation> getAllDonations(String token,
                                          UserService userService) {
        userService.getUserByToken(token);
        return donationRepository.findAll();
    }

    public List<Donation> getDonationsByFund(String token, Long fundId,
                                             UserService userService) {
        userService.getUserByToken(token);
        return donationRepository.findByFundId(fundId);
    }

    public List<Donation> getDonationsByProject(String token, Long projectId,
                                                UserService userService) {
        userService.getUserByToken(token);
        return donationRepository.findByProjectId(projectId);
    }

    public Donation registerDonation(String token, Donation donation,
                                     UserService userService) {
        User caller = userService.getUserByToken(token);
        validateDonation(donation);
        donation.setRegisteredByUserId(caller.getId());

        Donation saved = donationRepository.save(donation);

        BigDecimal newTotal = donationRepository.sumByFundId(
                donation.getFundId());
        fundRepository.findById(donation.getFundId()).ifPresent(fund -> {
            fund.setTotalReceived(newTotal);
            fundRepository.updateTotals(fund.getId(),
                    fund.getTotalReceived(), fund.getTotalSpent());
        });

        if (donation.getProjectId() != null)
            projectService.addRaisedAmount(
                    donation.getProjectId(), donation.getAmount());

        return saved;
    }

    public void deleteDonation(String token, Long donationId,
                               UserService userService) {
        userService.requireRole(token, Role.ADMIN);
        donationRepository.deleteById(donationId);
    }

    /**
     * Возвращает топ-3 донатёров по суммарному объёму пожертвований.
     * Анонимные исключаются.
     */
    public List<Map<String, Object>> getTopDonors(String token,
                                                  UserService userService) {
        userService.getUserByToken(token);
        List<Donation> all = donationRepository.findAll();

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Donation d : all) {
            if (d.isAnonymous()) continue;
            String name = d.getDonorName();
            totals.merge(name, d.getAmount(), BigDecimal::add);
        }

        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue()
                        .reversed())
                .limit(3)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("donorName", e.getKey());
                    m.put("totalAmount", e.getValue());
                    // Количество пожертвований
                    long count = all.stream()
                            .filter(d -> !d.isAnonymous()
                                    && e.getKey().equals(d.getDonorName()))
                            .count();
                    m.put("count", count);
                    return m;
                })
                .collect(Collectors.toList());
    }

    public DonationStatistics getDonationStatistics(String token,
                                                    Long fundId,
                                                    UserService userService) {
        userService.getUserByToken(token);
        List<Donation> donations = fundId != null
                ? donationRepository.findByFundId(fundId)
                : donationRepository.findAll();

        BigDecimal total = donations.stream()
                .map(Donation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avg = donations.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(donations.size()),
                2, RoundingMode.HALF_UP);

        BigDecimal max = donations.stream()
                .map(Donation::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new DonationStatistics(donations.size(), total, avg, max);
    }

    private void validateDonation(Donation donation) {
        if (donation.getFundId() == null)
            throw new CharityException.ValidationException(
                    "Необходимо указать фонд");
        if (donation.getDonorName() == null
                || donation.getDonorName().isBlank())
            throw new CharityException.ValidationException(
                    "Имя донора обязательно");
        if (donation.getAmount() == null
                || donation.getAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new CharityException.ValidationException(
                    "Сумма пожертвования должна быть больше 0");
        if (donation.getPaymentMethod() == null)
            throw new CharityException.ValidationException(
                    "Необходимо указать метод оплаты");
    }

    public record DonationStatistics(int count, BigDecimal total,
                                     BigDecimal average, BigDecimal max) {}
}