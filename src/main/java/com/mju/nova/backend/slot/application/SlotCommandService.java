package com.mju.nova.backend.slot.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.member.domain.StudentRole;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.domain.Semester;
import com.mju.nova.backend.slot.application.dto.AdminSlotResponse;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import com.mju.nova.backend.slot.domain.SlotRepository;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 수령 시간(슬롯) 등록·수정·열고 닫기 (Command).
 * 삭제는 없다. 대여 기록이 슬롯을 참조하고 있어서, 쓰지 않는 슬롯은 닫는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SlotCommandService {

    private final SlotRepository slotRepository;
    private final StudentRepository studentRepository;
    private final RentRepository rentRepository;
    private final SemesterQueryService semesterQueryService;

    // 학기는 현재 학기, 처음엔 열린 상태. 검사 순서는 명세 8. 수령 시간 등록 처리 규칙 순서 그대로
    public AdminSlotResponse create(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                    String managerStudentId) {
        validateTime(startTime, endTime);
        Semester semester = semesterQueryService.findCurrent()
                .orElseThrow(() -> new BusinessException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
        Student manager = findManager(managerStudentId);
        if (slotRepository.existsBySemester_IdAndDayOfWeekAndStartTime(semester.getId(), dayOfWeek, startTime)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLOT);
        }
        try {
            // 동시에 같은 슬롯이 들어온 경우 UK(uk_slot_semester_id_day_of_week_start_time) 위반을 여기서 잡는다
            Slot slot = slotRepository.saveAndFlush(Slot.create(semester, manager, dayOfWeek, startTime, endTime));
            return AdminSlotResponse.of(slot, 0);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLOT);
        }
    }

    // PATCH: null인 칸은 그대로. 요일은 바꾸지 않는다. 검사 순서는 명세 8. 수령 시간 수정 처리 규칙 순서 그대로
    public AdminSlotResponse update(Long slotId, LocalTime startTime, LocalTime endTime, String managerStudentId) {
        Slot slot = findSlotForUpdate(slotId);
        LocalTime newStart = startTime != null ? startTime : slot.getStartTime();
        LocalTime newEnd = endTime != null ? endTime : slot.getEndTime();
        validateTime(newStart, newEnd);

        boolean timeChanged = !newStart.equals(slot.getStartTime()) || !newEnd.equals(slot.getEndTime());
        if (timeChanged) {
            // 예약한 학생이 알던 수령 시간이 바뀌면 안 된다. 담당자 변경은 허용
            if (rentRepository.existsBySlot_IdAndStatus(slotId, RentStatus.RESERVED)) {
                throw new BusinessException(ErrorCode.SLOT_HAS_RESERVATION);
            }
            if (!newStart.equals(slot.getStartTime()) && slotRepository.existsBySemester_IdAndDayOfWeekAndStartTimeAndIdNot(
                    slot.getSemester().getId(), slot.getDayOfWeek(), newStart, slotId)) {
                throw new BusinessException(ErrorCode.DUPLICATE_SLOT);
            }
            slot.changeTime(newStart, newEnd);
        }
        if (managerStudentId != null) {
            slot.changeManager(findManager(managerStudentId));
        }

        try {
            slotRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLOT);
        }
        return toResponse(slot);
    }

    // 닫아도 기존 예약은 그대로 두고, 새 대여 신청만 SL002로 막힌다. 응답의 reservedCount로 연락할 학생이 있는지 본다
    public AdminSlotResponse changeStatus(Long slotId, boolean open) {
        Slot slot = findSlotForUpdate(slotId);
        slot.changeStatus(open);
        slotRepository.flush();
        return toResponse(slot);
    }

    private AdminSlotResponse toResponse(Slot slot) {
        return AdminSlotResponse.of(slot, rentRepository.countBySlot_IdAndStatus(slot.getId(), RentStatus.RESERVED));
    }

    private void validateTime(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_SLOT_TIME);
        }
    }

    // 담당자는 DB의 role이 국장·회장단인 회원만. 탈퇴한 회원은 없는 회원으로 본다 (M001)
    private Student findManager(String studentId) {
        Student manager = studentRepository.findById(studentId)
                .filter(student -> !student.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (manager.getRole() != StudentRole.DIRECTOR && manager.getRole() != StudentRole.PRESIDENT) {
            throw new BusinessException(ErrorCode.SLOT_MANAGER_NOT_ADMIN);
        }
        return manager;
    }

    // 같은 슬롯으로 대여 신청이 들어오는 중이면 끝날 때까지 기다린다 (SL007 검사가 엇갈리지 않게)
    private Slot findSlotForUpdate(Long slotId) {
        return slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));
    }
}
