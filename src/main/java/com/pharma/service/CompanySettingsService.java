package com.pharma.service;

import com.pharma.model.CompanySettings;
import com.pharma.repository.CompanySettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class CompanySettingsService {

    private static final Long SINGLETON_ID = 1L;
    private static final String DEFAULT_TERMS =
            "1. All Subject to Jurisdiction Only.\n" +
            "2. Goods once sold will not be taken back or exchanged.\n" +
            "3. No claims for any breakage or expiry.";

    private final CompanySettingsRepository repo;

    public CompanySettingsService(CompanySettingsRepository repo) {
        this.repo = repo;
    }

    /** Always returns the single settings row, creating an empty one if needed. */
    public CompanySettings get() {
        return repo.findById(SINGLETON_ID).orElseGet(() -> {
            CompanySettings s = new CompanySettings();
            s.setId(SINGLETON_ID);
            s.setConfigured(false);
            s.setTermsAndConditions(DEFAULT_TERMS);
            return repo.save(s);
        });
    }

    public CompanySettings save(CompanySettings data) {
        CompanySettings s = get();
        s.setCompanyName(data.getCompanyName());
        s.setAddressLine1(data.getAddressLine1());
        s.setAddressLine2(data.getAddressLine2());
        s.setCity(data.getCity());
        s.setState(data.getState());
        s.setPincode(data.getPincode());
        s.setPhone(data.getPhone());
        s.setEmail(data.getEmail());
        s.setGstNumber(data.getGstNumber());
        s.setDlNumber(data.getDlNumber());
        s.setFssaiNumber(data.getFssaiNumber());
        s.setBankName(data.getBankName());
        s.setBankBranch(data.getBankBranch());
        s.setBankAccountNumber(data.getBankAccountNumber());
        s.setBankIfsc(data.getBankIfsc());
        s.setUpiId(data.getUpiId());
        if (data.getTermsAndConditions() != null && !data.getTermsAndConditions().isBlank()) {
            s.setTermsAndConditions(data.getTermsAndConditions());
        }
        s.setConfigured(true);
        return repo.save(s);
    }
}
