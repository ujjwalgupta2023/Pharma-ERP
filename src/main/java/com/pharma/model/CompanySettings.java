package com.pharma.model;

import jakarta.persistence.*;

/**
 * Singleton company profile shown on every printed bill. There is only ever one
 * row (id = 1). Captured via the first-time setup wizard in the UI.
 */
@Entity
@Table(name = "company_settings")
public class CompanySettings {

    @Id
    private Long id = 1L;

    private String companyName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String email;

    private String gstNumber;
    private String dlNumber;    // Drug licence number(s)
    private String fssaiNumber;

    // Bank details printed on the bill footer.
    private String bankName;
    private String bankBranch;
    private String bankAccountNumber;
    private String bankIfsc;
    private String upiId;

    private String termsAndConditions;

    /** Flag so the UI knows whether to show the first-time setup wizard. */
    private boolean configured = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGstNumber() { return gstNumber; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    public String getDlNumber() { return dlNumber; }
    public void setDlNumber(String dlNumber) { this.dlNumber = dlNumber; }

    public String getFssaiNumber() { return fssaiNumber; }
    public void setFssaiNumber(String fssaiNumber) { this.fssaiNumber = fssaiNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }
}
