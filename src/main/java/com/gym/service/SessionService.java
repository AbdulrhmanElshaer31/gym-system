package com.gym.service;

import com.gym.entity.Session;
import com.gym.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    public List<Session> getAllSessions() {
        return sessionRepository.findAllOrderByActiveAndPrice();
    }

    public List<Session> getActiveSessions() {
        return sessionRepository.findByActiveTrueOrderByPriceAsc();
    }

    public Optional<Session> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    @Transactional
    public Session createSession(Session session) {
        session.setActive(true);
        session.setVersion(0L); // Initialize version explicitly
        return sessionRepository.save(session);
    }

    @Transactional
    public Session updateSession(Long id, Session updatedSession) {
        return sessionRepository.findById(id)
                .map(session -> {
                    session.setName(updatedSession.getName());
                    session.setNumberOfSessions(updatedSession.getNumberOfSessions());
                    session.setPrice(updatedSession.getPrice());
                    session.setRenewalPrice(updatedSession.getRenewalPrice());
                    session.setDescription(updatedSession.getDescription());
                    session.setActive(updatedSession.getActive());
                    return sessionRepository.save(session);
                })
                .orElseThrow(() -> new RuntimeException("الحصة غير موجودة"));
    }

    @Transactional
    public void toggleSessionStatus(Long id) {
        sessionRepository.findById(id)
                .ifPresent(session -> {
                    session.setActive(!session.getActive());
                    sessionRepository.save(session);
                });
    }

    @Transactional
    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);
    }
}