package by.charity.shared.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Donation implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PaymentMethod implements Serializable {
        BANK_TRANSFER("Банковский перевод"),
        CASH("Наличные"),
        ERIP("ЕРИП"),
        CARD("Банковская карта");

        private final String displayName;
        PaymentMethod(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }

    private Long id;
    private Long fundId;
    private String fundName;
    private Long projectId;        // nullable
    private String projectName;    // nullable
    private String donorName;
    private String donorEmail;
    private BigDecimal amount;     // BYN
    private PaymentMethod paymentMethod;
    private String comment;
    private boolean anonymous;
    private LocalDateTime donatedAt;
    private Long registeredByUserId;

    public Donation() {
        this.donatedAt = LocalDateTime.now();
        this.anonymous = false;
    }

    public Donation(Long fundId, String donorName, BigDecimal amount, PaymentMethod paymentMethod) {
        this();
        this.fundId = fundId;
        this.donorName = donorName;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    // Getters and setters
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

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getDonorEmail() { return donorEmail; }
    public void setDonorEmail(String donorEmail) { this.donorEmail = donorEmail; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }

    public LocalDateTime getDonatedAt() { return donatedAt; }
    public void setDonatedAt(LocalDateTime donatedAt) { this.donatedAt = donatedAt; }

    public Long getRegisteredByUserId() { return registeredByUserId; }
    public void setRegisteredByUserId(Long registeredByUserId) { this.registeredByUserId = registeredByUserId; }

    @Override
    public String toString() {
        return "Donation{id=" + id + ", amount=" + amount + " BYN, donor=" + donorName + "}";
    }
}