package by.charity.server.service;

import by.charity.server.repository.ProjectRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Project;
import by.charity.shared.model.Role;

import java.math.BigDecimal;
import java.util.List;

public class ProjectService {
    private final ProjectRepository projectRepository =
            new ProjectRepository();

    public List<Project> getAllProjects(String token,
                                        UserService userService) {
        userService.getUserByToken(token);
        return projectRepository.findAll();
    }

    public List<Project> getProjectsByFund(String token, Long fundId,
                                           UserService userService) {
        userService.getUserByToken(token);
        return projectRepository.findByFundId(fundId);
    }

    public Project getProjectById(String token, Long id,
                                  UserService userService) {
        userService.getUserByToken(token);
        return projectRepository.findById(id)
                .orElseThrow(() ->
                        new CharityException.NotFoundException("Проект", id));
    }

    public Project createProject(String token, Project project,
                                 UserService userService) {
        // Только ADMIN и MANAGER
        userService.requireRole(token, Role.ADMIN, Role.MANAGER);
        validateProject(project);
        return projectRepository.save(project);
    }

    public void updateProject(String token, Project project,
                              UserService userService) {
        // Только ADMIN и MANAGER
        userService.requireRole(token, Role.ADMIN, Role.MANAGER);
        validateProject(project);
        projectRepository.update(project);
    }

    public void updateProjectStatus(String token, Long projectId,
                                    Project.Status status,
                                    UserService userService) {
        // Только ADMIN и MANAGER
        userService.requireRole(token, Role.ADMIN, Role.MANAGER);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new CharityException.NotFoundException(
                                "Проект", projectId));
        project.setStatus(status);
        projectRepository.update(project);
    }

    public void deleteProject(String token, Long projectId,
                              UserService userService) {
        userService.requireRole(token, Role.ADMIN);
        projectRepository.deleteById(projectId);
    }

    public void addRaisedAmount(Long projectId, BigDecimal amount) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new CharityException.NotFoundException(
                                "Проект", projectId));
        BigDecimal newRaised = project.getRaisedAmount().add(amount);
        projectRepository.updateRaised(projectId, newRaised);
    }

    private void validateProject(Project project) {
        if (project.getName() == null || project.getName().isBlank())
            throw new CharityException.ValidationException(
                    "Название проекта обязательно");
        if (project.getGoalAmount() == null
                || project.getGoalAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new CharityException.ValidationException(
                    "Целевая сумма должна быть больше 0");
        if (project.getFundId() == null)
            throw new CharityException.ValidationException(
                    "Необходимо указать фонд");
    }
}