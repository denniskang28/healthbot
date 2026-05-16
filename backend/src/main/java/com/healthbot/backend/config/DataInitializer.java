package com.healthbot.backend.config;

import com.healthbot.backend.model.*;
import com.healthbot.backend.repository.*;
import com.healthbot.backend.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final UserSessionRepository userSessionRepository;
    private final LlmConfigRepository llmConfigRepository;
    private final AdminCredentialRepository adminCredentialRepository;
    private final AdminAuthService adminAuthService;
    private final ServiceProviderRepository serviceProviderRepository;
    private final RoutingRuleRepository routingRuleRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User u1 = new User(); u1.setName("Alice Chen"); u1.setPhone("+1-555-0101"); u1.setLanguage("EN");
            User u2 = new User(); u2.setName("Bob Wang"); u2.setPhone("+1-555-0102"); u2.setLanguage("ZH");
            User u3 = new User(); u3.setName("Carol Liu"); u3.setPhone("+1-555-0103"); u3.setLanguage("EN");
            userRepository.save(u1);
            userRepository.save(u2);
            userRepository.save(u3);

            UserSession s1 = new UserSession(); s1.setUserId(u1.getId()); s1.setCurrentState("IDLE");
            UserSession s2 = new UserSession(); s2.setUserId(u2.getId()); s2.setCurrentState("IDLE");
            UserSession s3 = new UserSession(); s3.setUserId(u3.getId()); s3.setCurrentState("IDLE");
            userSessionRepository.save(s1);
            userSessionRepository.save(s2);
            userSessionRepository.save(s3);
        }

        if (doctorRepository.count() == 0) {
            Doctor d1 = new Doctor(); d1.setName("Dr. James Wilson"); d1.setSpecialty("Cardiology");
            d1.setBio("Board-certified cardiologist with 15 years of experience in interventional cardiology and heart disease prevention.");
            d1.setRating(4.9); d1.setAvailable(true); d1.setAvatarInitials("JW");
            doctorRepository.save(d1);

            Doctor d2 = new Doctor(); d2.setName("Dr. Sarah Chen"); d2.setSpecialty("General Practice");
            d2.setBio("Family medicine physician specializing in preventive care, chronic disease management, and general health consultations.");
            d2.setRating(4.8); d2.setAvailable(true); d2.setAvatarInitials("SC");
            doctorRepository.save(d2);

            Doctor d3 = new Doctor(); d3.setName("Dr. Michael Park"); d3.setSpecialty("Dermatology");
            d3.setBio("Dermatologist with expertise in skin conditions, cosmetic dermatology, and skin cancer screening.");
            d3.setRating(4.7); d3.setAvailable(true); d3.setAvatarInitials("MP");
            doctorRepository.save(d3);

            Doctor d4 = new Doctor(); d4.setName("Dr. Emily Zhang"); d4.setSpecialty("Neurology");
            d4.setBio("Neurologist specializing in headaches, migraines, epilepsy, and neurological disorders.");
            d4.setRating(4.9); d4.setAvailable(true); d4.setAvatarInitials("EZ");
            doctorRepository.save(d4);

            Doctor d5 = new Doctor(); d5.setName("Dr. David Kim"); d5.setSpecialty("Pediatrics");
            d5.setBio("Pediatrician with 12 years of experience providing comprehensive healthcare for children from newborns to adolescents.");
            d5.setRating(4.8); d5.setAvailable(true); d5.setAvatarInitials("DK");
            doctorRepository.save(d5);
        }

        if (adminCredentialRepository.count() == 0) {
            AdminCredential cred = new AdminCredential();
            cred.setPasswordHash(adminAuthService.hash("12345"));
            adminCredentialRepository.save(cred);
        }

        if (serviceProviderRepository.count() == 0) {
            ServiceProvider llm = new ServiceProvider();
            llm.setName("Anthropic Claude");
            llm.setType("MEDICAL_LLM");
            llm.setCompany("Anthropic");
            llm.setDescription("Advanced medical AI powered by Claude, providing symptom analysis, health guidance, and clinical decision support.");
            llm.setPriority(100);
            serviceProviderRepository.save(llm);

            ServiceProvider oc1 = new ServiceProvider();
            oc1.setName("MediConnect Online");
            oc1.setType("ONLINE_CONSULTATION");
            oc1.setCompany("MediConnect Health");
            oc1.setDescription("Leading online medical consultation platform with 10,000+ licensed physicians available 24/7.");
            oc1.setPriority(100);
            ServiceProvider savedOc1 = serviceProviderRepository.save(oc1);

            ServiceProvider oc2 = new ServiceProvider();
            oc2.setName("CareCloud Doctors");
            oc2.setType("ONLINE_CONSULTATION");
            oc2.setCompany("CareCloud Medical");
            oc2.setDescription("Cloud-based health consultation platform with AI-assisted triage and specialist matching.");
            oc2.setPriority(80);
            serviceProviderRepository.save(oc2);

            ServiceProvider oa1 = new ServiceProvider();
            oa1.setName("HealthNet Hospital Network");
            oa1.setType("OFFLINE_APPOINTMENT");
            oa1.setCompany("HealthNet Group");
            oa1.setDescription("Nationwide hospital appointment booking across 500+ partner hospitals in major cities.");
            oa1.setPriority(100);
            ServiceProvider savedOa1 = serviceProviderRepository.save(oa1);

            ServiceProvider oa2 = new ServiceProvider();
            oa2.setName("Premier Care Centers");
            oa2.setType("OFFLINE_APPOINTMENT");
            oa2.setCompany("Premier Health");
            oa2.setDescription("Premium multi-specialty medical centers with English-speaking doctors in major urban areas.");
            oa2.setPriority(80);
            serviceProviderRepository.save(oa2);

            ServiceProvider op1 = new ServiceProvider();
            op1.setName("QuickPharm Online");
            op1.setType("ONLINE_PHARMACY");
            op1.setCompany("QuickPharm");
            op1.setDescription("Online pharmacy with 2-hour delivery, integrated with health insurance for direct billing.");
            op1.setPriority(100);
            ServiceProvider savedOp1 = serviceProviderRepository.save(op1);

            ServiceProvider op2 = new ServiceProvider();
            op2.setName("MedMart Pharmacy");
            op2.setType("ONLINE_PHARMACY");
            op2.setCompany("MedMart");
            op2.setDescription("Certified online pharmacy with authentic medications, cold chain delivery, and 24/7 pharmacist support.");
            op2.setPriority(80);
            serviceProviderRepository.save(op2);

            RoutingRule r1 = new RoutingRule();
            r1.setName("ZH - Online Consultation");
            r1.setServiceType("ONLINE_CONSULTATION");
            r1.setConditionJson("{\"language\":\"ZH\"}");
            r1.setTargetProviderId(savedOc1.getId());
            r1.setPriority(100);
            routingRuleRepository.save(r1);

            RoutingRule r2 = new RoutingRule();
            r2.setName("ZH - Offline Appointment");
            r2.setServiceType("OFFLINE_APPOINTMENT");
            r2.setConditionJson("{\"language\":\"ZH\"}");
            r2.setTargetProviderId(savedOa1.getId());
            r2.setPriority(100);
            routingRuleRepository.save(r2);

            RoutingRule r3 = new RoutingRule();
            r3.setName("ZH - Online Pharmacy");
            r3.setServiceType("ONLINE_PHARMACY");
            r3.setConditionJson("{\"language\":\"ZH\"}");
            r3.setTargetProviderId(savedOp1.getId());
            r3.setPriority(100);
            routingRuleRepository.save(r3);
        }

        if (llmConfigRepository.count() == 0) {
            LlmConfig config = new LlmConfig();
            config.setProvider("anthropic");
            config.setModel("claude-sonnet-4-6");
            config.setApiUrl("http://localhost:8000");
            config.setApiKey("");
            config.setSystemPrompt("You are a helpful medical assistant for PingAn Health insurance app. Help users with medical questions and guide them to appropriate care. Be empathetic and professional.");
            config.setActive(true);
            llmConfigRepository.save(config);
        }
    }
}
