package by.charity.shared.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ReportType implements Serializable {
        MONTHLY("Ежемесячный"),
        QUARTERLY("Квартальный"),
        ANNUAL("Годовой"),
        PROJECT("По проекту");

        private final String displayName;
        ReportType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }

    private Long id;
    private Long fundId;
    private String fundName;
    private Long projectId;
    private String projectName;
    private ReportType type;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalReceived;
    private BigDecimal totalSpent;
    private int donationsCount;
    private String notes;
    private boolean isPublic;
    private Long createdByUserId;
    private LocalDateTime createdAt;

    public Report() {
        this.totalReceived = BigDecimal.ZERO;
        this.totalSpent = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.isPublic = false;
    }

    public BigDecimal getBalance() {
        return totalReceived.subtract(totalSpent);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFundId() { return fundId; }
    public void setFundId(Long fundId) { this.fundId = fundId; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public ReportType getType() { return type; }
    public void setType(ReportType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public BigDecimal getTotalReceived() { return totalReceived; }
    public void setTotalReceived(BigDecimal totalReceived) { this.totalReceived = totalReceived; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }

    public int getDonationsCount() { return donationsCount; }
    public void setDonationsCount(int donationsCount) { this.donationsCount = donationsCount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}