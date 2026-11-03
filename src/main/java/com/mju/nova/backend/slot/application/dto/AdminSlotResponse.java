package com.mju.nova.backend.slot.application.dto;

import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import java.time.LocalTime;

/**
 * 관리자용 수령 시간 (조회·등록·수정·열고 닫기 응답 공통).
 * reservedCount = 이 슬롯에 걸린 RESERVED 대여 수. 슬롯을 닫거나 시각을 바꾸기 전에 예약한 학생이 있는지 본다.
 */
public record AdminSlotResponse(
        Long slotId,
        Long semesterId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Boolean status,
        Manager manager,
        long reservedCount
) {
    // semester·manager는 LAZY지만 semester는 id만 쓰므로 추가 SELECT가 없다
    public static AdminSlotResponse of(Slot slot, long reservedCount) {
        return new AdminSlotResponse(slot.getId(), slot.getSemester().getId(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime(), slot.getStatus(), Manager.from(slot.getManager()),
                reservedCount);
    }

    public record Manager(
            String studentId,
            String name
    ) {
        static Manager from(Student student) {
            return new Manager(student.getId(), student.getName());
        }
    }
}
