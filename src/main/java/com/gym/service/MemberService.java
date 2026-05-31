package com.gym.service;

import com.gym.entity.*;
import com.gym.exception.MemberCreationException;
import com.gym.exception.SubscriptionException;
import com.gym.repository.MemberRepository;
import com.gym.repository.PlanRepository;
import com.gym.repository.SubscriptionHistoryRepository;
import com.gym.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    public List<Member> getAllActiveMembers() {
        log.debug("Fetching all active members with eager loading");
        try {
            return memberRepository.findAllActiveWithAssociations();
        } catch (Exception e) {
            log.error("Failed to fetch active members", e);
            throw e;
        }
    }

    public Optional<Member> getMemberById(Long id) {
        return memberRepository.findById(id);
    }

    public List<Member> searchMembers(String search) {
        if (search == null || search.trim().isEmpty()) {
            return getAllActiveMembers();
        }
        return memberRepository.searchMembers(search.trim());
    }

    private String generateUniqueMemberId() {
        String memberId;
        int attempt = 0;
        do {
            Long count = memberRepository.count() + attempt;
            memberId = String.format("GYM%03d", count + 1);
            attempt++;
        } while (memberRepository.findByMemberId(memberId).isPresent() && attempt < 1000);

        return memberId;
    }

    @Transactional
    public Member createMember(Member member, Plan plan) {
        log.info("Creating new member: {}", member.getName());
        try {
            // احصل على Plan managed من قاعدة البيانات
            Plan managedPlan = planRepository.findById(plan.getId())
                    .orElseThrow(() -> {
                        log.error("Plan not found: {}", plan.getId());
                        return new RuntimeException("الخطة غير موجودة");
                    });

            // توليد معرف فريد للعضو
            if (member.getMemberId() == null || member.getMemberId().isEmpty()) {
                member.setMemberId(generateUniqueMemberId());
            }

            member.setCurrentPlan(managedPlan);
            member.setSubscriptionStartDate(LocalDate.now());
            member.setSubscriptionEndDate(LocalDate.now().plusDays(managedPlan.getDurationDays()));
            member.setRenewalCount(0);
            member.setDeleted(false);

            // ✅ تعيين الحصص الأولية
            if (managedPlan.getNumberOfSessions() != null && managedPlan.getNumberOfSessions() > 0) {
                member.setRemainingSession(managedPlan.getNumberOfSessions());
            } else {
                member.setRemainingSession(0);
            }

            Member savedMember = memberRepository.save(member);
            log.debug("Member created with ID: {}", savedMember.getId());

            // إنشاء سجل الاشتراك
            SubscriptionHistory history = new SubscriptionHistory();
            history.setMember(savedMember);
            history.setPlan(managedPlan);
            history.setStartDate(savedMember.getSubscriptionStartDate());
            history.setEndDate(savedMember.getSubscriptionEndDate());
            history.setAmountPaid(managedPlan.getPrice());
            subscriptionHistoryRepository.save(history);

            // إنشاء معاملة مالية
            Transaction transaction = new Transaction();
            transaction.setType(Transaction.TransactionType.INCOME);
            transaction.setCategory(Transaction.TransactionCategory.SUBSCRIPTION);
            transaction.setAmount(managedPlan.getPrice());
            transaction.setDescription("اشتراك جديد - " + member.getName() + " - " + managedPlan.getName());
            transaction.setMember(savedMember);
            transaction.setTransactionDate(LocalDate.now());
            transactionRepository.save(transaction);

            return savedMember;
        } catch (DataIntegrityViolationException e) {
            log.error("Phone number already exists: {}", member.getPhone(), e);
            throw new MemberCreationException("رقم الهاتف موجود بالفعل", e);
        } catch (Exception e) {
            log.error("Failed to create member", e);
            throw new MemberCreationException("فشل إنشاء المشترك", e);
        }
    }

    @Transactional
    public Member updateMember(Long id, Member updatedMember) {
        return memberRepository.findById(id)
                .map(member -> {
                    // تحديث البيانات فقط
                    if (updatedMember.getMemberId() != null && !updatedMember.getMemberId().isEmpty()) {
                        member.setMemberId(updatedMember.getMemberId());
                    }
                    member.setName(updatedMember.getName());
                    member.setPhone(updatedMember.getPhone());
                    member.setCoach(updatedMember.getCoach());
                    member.setNotes(updatedMember.getNotes());
                    return memberRepository.save(member);
                })
                .orElseThrow(() -> new RuntimeException("المشترك غير موجود"));
    }

    @Transactional
    public void softDeleteMember(Long id) {
        memberRepository.findById(id)
                .ifPresent(member -> {
                    member.setDeleted(true);
                    memberRepository.save(member);
                });
    }

    @Transactional
    public Member renewSubscription(Long memberId, Plan newPlan) {
        return renewSubscription(memberId, newPlan, null);
    }

    @Transactional
    public Member renewSubscription(Long memberId, Plan newPlan, BigDecimal customAmount) {
        log.info("Renewing subscription for member {}", memberId);
        try {
            return memberRepository.findById(memberId)
                    .map(member -> {
                        Plan managedPlan = planRepository.findById(newPlan.getId())
                                .orElseThrow(() -> {
                                    log.error("Plan not found: {}", newPlan.getId());
                                    return new RuntimeException("الخطة غير موجودة");
                                });

                        LocalDate newStartDate = member.getSubscriptionEndDate().isAfter(LocalDate.now())
                                ? member.getSubscriptionEndDate()
                                : LocalDate.now();
                        LocalDate newEndDate = newStartDate.plusDays(managedPlan.getDurationDays());

                        member.setCurrentPlan(managedPlan);
                        member.setSubscriptionStartDate(newStartDate);
                        member.setSubscriptionEndDate(newEndDate);
                        member.setRenewalCount(member.getRenewalCount() + 1);

                        // ✅ إعادة تعيين الحصص إلى العدد الكامل
                        if (managedPlan.getNumberOfSessions() != null && managedPlan.getNumberOfSessions() > 0) {
                            member.setRemainingSession(managedPlan.getNumberOfSessions());
                        }

                        Member savedMember = memberRepository.save(member);
                        log.debug("Subscription renewed for member {}", memberId);

                        BigDecimal renewalAmount = customAmount != null ? customAmount : managedPlan.getEffectiveRenewalPrice();

                        // إنشاء سجل الاشتراك
                        SubscriptionHistory history = new SubscriptionHistory();
                        history.setMember(savedMember);
                        history.setPlan(managedPlan);
                        history.setStartDate(newStartDate);
                        history.setEndDate(newEndDate);
                        history.setAmountPaid(renewalAmount);
                        subscriptionHistoryRepository.save(history);

                        // إنشاء معاملة مالية
                        Transaction transaction = new Transaction();
                        transaction.setType(Transaction.TransactionType.INCOME);
                        transaction.setCategory(Transaction.TransactionCategory.RENEWAL);
                        transaction.setAmount(renewalAmount);
                        transaction.setDescription("تجديد اشتراك - " + member.getName() + " - " + managedPlan.getName());
                        transaction.setMember(savedMember);
                        transaction.setTransactionDate(LocalDate.now());
                        transactionRepository.save(transaction);

                        return savedMember;
                    })
                    .orElseThrow(() -> {
                        log.error("Member not found: {}", memberId);
                        return new SubscriptionException("المشترك غير موجود");
                    });
        } catch (Exception e) {
            log.error("Failed to renew subscription for member {}", memberId, e);
            throw new SubscriptionException("فشل تجديد الاشتراك", e);
        }
    }

    public List<SubscriptionHistory> getMemberHistory(Long memberId) {
        return subscriptionHistoryRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    public Long countActiveMembers() {
        return memberRepository.countActiveMembers(LocalDate.now());
    }

    public Long countExpiredMembers() {
        return memberRepository.countExpiredMembers(LocalDate.now());
    }

    public List<Member> getExpiredMembers() {
        return memberRepository.findExpiredMembers(LocalDate.now());
    }

    public List<Member> getMembersNearExpiry() {
        return memberRepository.findMembersNearExpiry(LocalDate.now(), LocalDate.now().plusDays(7));
    }

    public List<Member> getMembersByPlan(Long planId) {
        return memberRepository.findByPlanId(planId);
    }

    // ✅ البحث عن المشترك بـ ID string
    public Optional<Member> findByMemberId(String memberId) {
        return memberRepository.findByMemberId(memberId);
    }

    // ✅ تسجيل حضور (ينقص الحصص)
    @Transactional
    public void recordAttendance(Long memberId) {
        memberRepository.findById(memberId)
                .ifPresent(member -> {
                    // Check if member has sessions limit
                    if (member.getCurrentPlan() == null ||
                            member.getCurrentPlan().getNumberOfSessions() == null ||
                            member.getCurrentPlan().getNumberOfSessions() == 0) {
                        throw new RuntimeException("هذه الخطة بدون حصص محددة");
                    }

                    // Check if sessions available
                    if (member.getRemainingSession() <= 0) {
                        throw new RuntimeException("انتهت الحصص! يرجى تجديد الاشتراك");
                    }

                    // Deduct one session
                    member.setRemainingSession(member.getRemainingSession() - 1);
                    memberRepository.save(member);

                    // Log in audit
                    auditService.logAction(
                            "Member",
                            memberId,
                            AuditLog.AuditAction.STOCK_ADJUST,
                            "sessions: " + (member.getRemainingSession() + 1),
                            "sessions: " + member.getRemainingSession(),
                            "تسجيل حضور - " + member.getName()
                    );
                });
    }

    // ✅ FIXED: البحث عن المشترك بدون تحقق من الحالة - يعرض أي حال كانت
    public Member findByMemberIdWithValidation(String memberId) {
        return memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new RuntimeException("المشترك غير موجود"));
    }
}
