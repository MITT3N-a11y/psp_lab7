package by.charity.server.service;

import by.charity.server.repository.ProjectRepository;
import by.charity.shared.exception.CharityException;
import by.charity.shared.model.Project;
import by.charity.shared.model.Role;
import by.charity.shared.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты ProjectService")
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    private ProjectService projectService;

    private User managerUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService();
        try {
            var field = ProjectService.class.getDeclaredField("projectRepository");
            field.setAccessible(true);
            field.set(projectService, projectRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        managerUser = new User();
        managerUser.setId(1L);
        managerUser.setRole(Role.MANAGER);
        managerUser.setActive(true);

        testProject = new Project();
        testProject.setId(1L);
        testProject.setFundId(1L);
        testProject.setName("Тестовый проект");
        testProject.setGoalAmount(new BigDecimal("10000.00"));
        testProject.setRaisedAmount(new BigDecimal("5000.00"));
        testProject.setStatus(Project.Status.ACTIVE);
        testProject.setStartDate(LocalDate.now());
        testProject.setEndDate(LocalDate.now().plusMonths(3));
    }

    @Test
    @DisplayName("Создание проекта с корректными данными выполняется успешно")
    void createProject_valid_savedSuccessfully() {
        doNothing().when(userService).requireRole("token", Role.ADMIN, Role.MANAGER);
        when(projectRepository.save(any())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        Project newProject = new Project();
        newProject.setFundId(1L);
        newProject.setName("Новый проект");
        newProject.setGoalAmount(new BigDecimal("5000.00"));

        Project result = projectService.createProject("token", newProject, userService);

        assertNotNull(result.getId());
        verify(projectRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Создание проекта без названия запрещено")
    void createProject_emptyName_throwsValidationException() {
        doNothing().when(userService).requireRole("token", Role.ADMIN, Role.MANAGER);

        Project invalid = new Project();
        invalid.setFundId(1L);
        invalid.setName("");
        invalid.setGoalAmount(new BigDecimal("1000.00"));

        assertThrows(CharityException.ValidationException.class,
                () -> projectService.createProject("token", invalid, userService));
    }

    @Test
    @DisplayName("Создание проекта с нулевой целевой суммой запрещено")
    void createProject_zeroGoal_throwsValidationException() {
        doNothing().when(userService).requireRole("token", Role.ADMIN, Role.MANAGER);

        Project invalid = new Project();
        invalid.setFundId(1L);
        invalid.setName("Проект");
        invalid.setGoalAmount(BigDecimal.ZERO);

        assertThrows(CharityException.ValidationException.class,
                () -> projectService.createProject("token", invalid, userService));
    }

    @Test
    @DisplayName("Создание проекта без привязки к фонду запрещено")
    void createProject_nullFundId_throwsValidationException() {
        doNothing().when(userService).requireRole("token", Role.ADMIN, Role.MANAGER);

        Project invalid = new Project();
        invalid.setFundId(null);
        invalid.setName("Проект");
        invalid.setGoalAmount(new BigDecimal("1000.00"));

        assertThrows(CharityException.ValidationException.class,
                () -> projectService.createProject("token", invalid, userService));
    }

    @Test
    @DisplayName("Прогресс проекта рассчитывается корректно")
    void project_getProgressPercent_correct() {
        double progress = testProject.getProgressPercent();
        assertEquals(50.0, progress, 0.01);
    }

    @Test
    @DisplayName("Прогресс проекта не превышает 100%")
    void project_getProgressPercent_notOver100() {
        testProject.setRaisedAmount(new BigDecimal("15000.00"));
        double progress = testProject.getProgressPercent();
        assertTrue(progress >= 100.0);
    }

    @Test
    @DisplayName("Добавление суммы к проекту обновляет raised_amount")
    void addRaisedAmount_valid_updatesProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

        projectService.addRaisedAmount(1L, new BigDecimal("500.00"));

        verify(projectRepository, times(1))
                .updateRaised(eq(1L), eq(new BigDecimal("5500.00")));
    }

    @Test
    @DisplayName("Обновление статуса несуществующего проекта выбрасывает NotFoundException")
    void updateProjectStatus_notFound_throwsNotFoundException() {
        doNothing().when(userService).requireRole("token", Role.ADMIN, Role.MANAGER);
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CharityException.NotFoundException.class,
                () -> projectService.updateProjectStatus(
                        "token", 999L, Project.Status.COMPLETED, userService));
    }
}