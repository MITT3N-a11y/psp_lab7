package by.charity.shared.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status implements Serializable {
        PLANNED("Планируется"),
        ACTIVE("Активный"),
        COMPLETED("Завершён"),
        SUSPENDED("Приостановлен");

        private final String displayName;
        Status(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }

    private Long id;
    private Long fundId;
    private String fundName;
    private String name;
    private String description;
    private BigDecimal goalAmount;   // BYN
    private BigDecimal raisedAmount; // BYN
    private Status status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    public Project() {
        this.raisedAmount = BigDecimal.ZERO;
        this.status = Status.PLANNED;
        this.createdAt = LocalDateTime.now();
    }

    public Project(Long fundId, String name, String description,
                   BigDecimal goalAmount, LocalDate startDate, LocalDate endDate) {
        this();
        this.fundId = fundId;
        this.name = name;
        this.description = description;
        this.goalAmount = goalAmount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public double getProgressPercent() {
        if (goalAmount == null || goalAmount.compareTo(BigDecimal.ZERO) == 0) return 0;
        return raisedAmount.divide(goalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getGoalAmount() { return goalAmount; }
    public void setGoalAmount(BigDecimal goalAmount) { this.goalAmount = goalAmount; }

    public BigDecimal getRaisedAmount() { return raisedAmount; }
    public void setRaisedAmount(BigDecimal raisedAmount) { this.raisedAmount = raisedAmount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}