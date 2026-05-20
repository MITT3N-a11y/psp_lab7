package by.charity.server.service;

import by.charity.server.repository.DonationRepository;
import by.charity.server.repository.ReportRepository;
import by.charity.server.repository.UserRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;
import by.charity.shared.util.PasswordUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {
    private final UserRepository     userRepository     = new UserRepository();
    private final DonationRepository donationRepository = new DonationRepository();
    private final ReportRepository   reportRepository   = new ReportRepository();
    private final Map<String, User>  activeSessions     = new ConcurrentHashMap<>();

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new CharityException.AuthException(
                                "Пользователь не найден"));
        if (!user.isActive())
            throw new CharityException.AuthException(
                    "Учётная запись деактивирована");
        if (!PasswordUtil.hashPassword(password).equals(user.getPasswordHash()))
            throw new CharityException.AuthException("Неверный пароль");
        String token = UUID.randomUUID().toString();
        activeSessions.put(token, user);
        return token;
    }

    public void logout(String token) {
        activeSessions.remove(token);
    }

    public User getUserByToken(String token) {
        if (token == null)
            throw new CharityException.AuthException("Токен не предоставлен");
        User user = activeSessions.get(token);
        if (user == null)
            throw new CharityException.AuthException(
                    "Сессия истекла или не найдена");
        return user;
    }

    public void requireRole(String token, Role... allowedRoles) {
        User user = getUserByToken(token);
        for (Role role : allowedRoles)
            if (user.getRole() == role) return;
        throw new CharityException.AuthException(
                "Недостаточно прав доступа");
    }

    public List<User> getAllUsers(String token) {
        requireRole(token, Role.ADMIN);
        return userRepository.findAll();
    }

    public User getUserById(String token, Long id) {
        requireRole(token, Role.ADMIN, Role.MANAGER);
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new CharityException.NotFoundException(
                                "Пользователь", id));
    }

    public User createUser(String token, User newUser) {
        requireRole(token, Role.ADMIN);
        if (userRepository.findByUsername(newUser.getUsername()).isPresent())
            throw new CharityException.ValidationException(
                    "Пользователь с таким именем уже существует");
        newUser.setPasswordHash(PasswordUtil.hashPassword("changeme123"));
        return userRepository.save(newUser);
    }

    public User registerGuest(User newUser) {
        if (userRepository.findByUsername(newUser.getUsername()).isPresent())
            throw new CharityException.ValidationException(
                    "Пользователь с таким логином уже существует");
        newUser.setRole(Role.GUEST);
        newUser.setActive(true);
        return userRepository.save(newUser);
    }

    public void updateUser(String token, User user) {
        requireRole(token, Role.ADMIN);
        userRepository.update(user);
    }

    public void updateProfile(String token, User user) {
        User caller = getUserByToken(token);
        user.setId(caller.getId());
        user.setRole(caller.getRole());
        user.setActive(caller.isActive());
        userRepository.update(user);
        caller.setFullName(user.getFullName());
        caller.setEmail(user.getEmail());
        caller.setDescription(user.getDescription());
    }

    public void changePassword(String token, Long userId,
                               String oldPassword, String newPassword) {
        User caller = getUserByToken(token);
        if (!caller.getId().equals(userId) && caller.getRole() != Role.ADMIN)
            throw new CharityException.AuthException(
                    "Нет прав менять чужой пароль");
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CharityException.NotFoundException(
                                "Пользователь", userId));
        if (caller.getRole() != Role.ADMIN) {
            if (!PasswordUtil.hashPassword(oldPassword)
                    .equals(user.getPasswordHash()))
                throw new CharityException.AuthException(
                        "Неверный текущий пароль");
        }
        if (newPassword == null || newPassword.length() < 6)
            throw new CharityException.ValidationException(
                    "Пароль должен содержать минимум 6 символов");
        userRepository.updatePassword(userId,
                PasswordUtil.hashPassword(newPassword));
    }

    public void deactivateUser(String token, Long userId) {
        requireRole(token, Role.ADMIN);
        User caller = getUserByToken(token);
        if (caller.getId().equals(userId))
            throw new CharityException.ValidationException(
                    "Нельзя деактивировать собственную учётную запись");
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CharityException.NotFoundException(
                                "Пользователь", userId));
        user.setActive(false);
        userRepository.update(user);
    }

    public void deleteUser(String token, Long userId) {
        requireRole(token, Role.ADMIN);
        User caller = getUserByToken(token);
        if (caller.getId().equals(userId))
            throw new CharityException.ValidationException(
                    "Нельзя удалить собственную учётную запись");
        // Обнуляем ссылки во всех связанных таблицах,
        // чтобы сохранить финансовую историю
        donationRepository.nullifyRegisteredByUserId(userId);
        reportRepository.nullifyCreatedByUserId(userId);
        // Удаляем пользователя
        userRepository.deleteById(userId);
        // Убрать сессию если пользователь был онлайн
        activeSessions.entrySet().removeIf(
                e -> e.getValue().getId().equals(userId));
    }

    public static String hashPassword(String password) {
        return PasswordUtil.hashPassword(password);
    }
}