package by.charity.server.service;

import by.charity.server.repository.DonationRepository;
import by.charity.server.repository.FundRepository;
import by.charity.server.repository.ProjectRepository;
import by.charity.server.repository.ReportRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.CharityFund;
import by.charity.shared.model.Role;

import java.util.List;
import java.math.BigDecimal;

public class FundService {
    private final FundRepository     fundRepository     = new FundRepository();
    private final DonationRepository donationRepository = new DonationRepository();
    private final ProjectRepository  projectRepository  = new ProjectRepository();
    private final ReportRepository   reportRepository   = new ReportRepository();

    public List<CharityFund> getAllFunds(String token,
                                         UserService userService) {
        userService.getUserByToken(token);
        return fundRepository.findAll();
    }

    public CharityFund getFundById(String token, Long id,
                                   UserService userService) {
        userService.getUserByToken(token);
        return fundRepository.findById(id)
                .orElseThrow(() ->
                        new CharityException.NotFoundException("Фонд", id));
    }

    public CharityFund createFund(String token, CharityFund fund,
                                  UserService userService) {
        userService.requireRole(token, Role.ADMIN, Role.MANAGER);
        validateFund(fund);
        return fundRepository.save(fund);
    }

    public void updateFund(String token, CharityFund fund,
                           UserService userService) {
        userService.requireRole(token, Role.ADMIN, Role.MANAGER);
        validateFund(fund);
        fundRepository.update(fund);
    }

    public void deactivateFund(String token, Long fundId,
                               UserService userService) {
        userService.requireRole(token, Role.ADMIN);
        CharityFund fund = fundRepository.findById(fundId)
                .orElseThrow(() ->
                        new CharityException.NotFoundException("Фонд", fundId));
        fund.setActive(false);
        fundRepository.update(fund);
    }

    public void deleteFund(String token, Long fundId,
                           UserService userService) {
        userService.requireRole(token, Role.ADMIN);
        // Удаляем дочерние записи перед удалением фонда
        reportRepository.deleteByFundId(fundId);
        donationRepository.deleteByFundId(fundId);
        projectRepository.deleteByFundId(fundId);
        fundRepository.deleteById(fundId);
    }

    private void validateFund(CharityFund fund) {
        if (fund.getName() == null || fund.getName().isBlank())
            throw new CharityException.ValidationException(
                    "Название фонда обязательно");
    }

    public void addExpense(String token, Long fundId, BigDecimal amount,
                           UserService userService) {
        userService.requireRole(token, Role.ADMIN, Role.MANAGER);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new CharityException.ValidationException(
                    "Сумма расхода должна быть больше 0");
        fundRepository.findById(fundId)
                .orElseThrow(() ->
                        new CharityException.NotFoundException("Фонд", fundId));
        fundRepository.addExpense(fundId, amount);
    }
}