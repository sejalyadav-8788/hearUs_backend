package com.hearUs.backend.security;

import com.hearUs.backend.entity.complaint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EscalationChain {

    private static final List<String> DEFAULT =
            List.of(Roles.COORDINATOR, Roles.HOD, Roles.DEAN, Roles.ADMIN);
    private static final List<String> HOSTEL =
            List.of(Roles.WARDEN, Roles.DEAN, Roles.ADMIN);
    private static final List<String> SENSITIVE =
            List.of(Roles.DEAN, Roles.ADMIN);

    public Optional<String> roleAt(complaint c, int level) {
        List<String> chain = c.isSensitive() ? SENSITIVE
                : "HOSTEL".equalsIgnoreCase(c.getCategory()) ? HOSTEL : DEFAULT;
        return level < chain.size() ? Optional.of(chain.get(level)) : Optional.empty();
    }
}