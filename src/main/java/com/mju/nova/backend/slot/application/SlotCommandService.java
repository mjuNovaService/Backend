package com.mju.nova.backend.slot.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.member.domain.StudentRole;
import com.mju.nova.backend.rental.domain.RentRepository;
import com.mju.nova.backend.rental.domain.RentStatus;
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
    private final CurrentSemesterReader currentSemesterReader;

    // 학기는 현재 학기, 처음엔 열린 상태
    public AdminSlotResponse create(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                    String managerStudentId) {
        validateTime(startTime, endTime);
        Student manager = findManager(managerStudentId);
        // 현재 학기가 없으면 대여 신청과 같이 SL003
        Semester semester = currentSemesterReader.find()
                .orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_IN_CURRENT_SEMESTER));
        if (slotRepository.existsBySemester_IdAndDayOfWeekAndStartTime(semester.getId(), dayOfWeek, startTime)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLOT);
        }
        try {
            // 동시에 같은 슬롯이 들어온 경우 UK(uk_slot_semester_id_day_of_week_start_time) 위반을 여기서 잡는다
            Slot slot = slotRepository.saveAndFlush(Slot.create(semester, manager, dayOfWeek, startTime, endTime));
            return AdminSlotResponse.from(slot);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLOT);
        }
    }

    // PATCH: null인 칸은 그대로. 요일은 바꾸지 않는다
    public AdminSlotResponse update(Long slotId, LocalTime startTime, LocalTime endTime, String managerStudentId) {
        Slot slot = findSlotForUpdate(slotId);
        LocalTime newStart = startTime != null ? startTime : slot.getStartTime();
        LocalTime newEnd = endTime != null ? endTime : slot.getEndTime();
        validateTime(newStart, newEnd);
        Student manager = managerStudentId != null ? findManager(managerStudentId) : null;

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
        if (manager != null) {
            slot.changeManager(manager);
        }

        try {
            slotRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLOT);
        }
        return AdminSlotResponse.from(slot);
    }

    // 닫아도 기존 예약은 그대로 두고, 새 대여 신청만 SL002로 막힌다
    public AdminSlotResponse changeStatus(Long slotId, boolean open) {
        Slot slot = findSlotForUpdate(slotId);
        slot.changeStatus(open);
        slotRepository.flush();
        return AdminSlotResponse.from(slot);
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
