package com.gym.service;

import com.gym.entity.Coach;
import com.gym.repository.CoachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoachService {

    private final CoachRepository coachRepository;

    public List<Coach> getAllActiveCoaches() {
        return coachRepository.findAllActiveCoaches();
    }

    public List<Coach> getAllWorkingCoaches() {
        return coachRepository.findAllWorkingCoaches();
    }

    public Optional<Coach> getCoachById(Long id) {
        return coachRepository.findById(id);
    }

    public List<Coach> searchCoaches(String search) {
        if (search == null || search.trim().isEmpty()) {
            return getAllActiveCoaches();
        }
        return coachRepository.searchCoaches(search.trim());
    }

    @Transactional
    public Coach createCoach(Coach coach) {
        coach.setActive(true);
        coach.setDeleted(false);
        return coachRepository.save(coach);
    }

    @Transactional
    public Coach updateCoach(Long id, Coach updatedCoach) {
        return coachRepository.findById(id)
                .map(coach -> {
                    coach.setName(updatedCoach.getName());
                    coach.setSpecialty(updatedCoach.getSpecialty());
                    coach.setSalary(updatedCoach.getSalary());
                    coach.setNotes(updatedCoach.getNotes());
                    coach.setActive(updatedCoach.getActive());
                    return coachRepository.save(coach);
                })
                .orElseThrow(() -> new RuntimeException("المدرب غير موجود"));
    }

    @Transactional
    public void toggleCoachStatus(Long id) {
        coachRepository.findById(id)
                .ifPresent(coach -> {
                    coach.setActive(!coach.getActive());
                    coachRepository.save(coach);
                });
    }

    @Transactional
    public void softDeleteCoach(Long id) {
        coachRepository.findById(id)
                .ifPresent(coach -> {
                    coach.setDeleted(true);
                    coachRepository.save(coach);
                });
    }

    public Long getMemberCountForCoach(Long coachId) {
        return coachRepository.countMembersByCoach(coachId);
    }
}