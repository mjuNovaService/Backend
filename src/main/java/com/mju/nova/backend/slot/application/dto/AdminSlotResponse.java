package com.mju.nova.backend.slot.application.dto;

import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import com.mju.nova.backend.slot.domain.Slot;
import java.time.LocalTime;

// 관리자용 수령 시간. 열림 여부와 담당자까지 보여준다 (조회·등록·수정 응답 공통)
public record AdminSlotResponse(
        Long slotId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Boolean status,
        Manager manager
) {
    public static AdminSlotResponse from(Slot slot) {
        return new AdminSlotResponse(slot.getId(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(),
                slot.getStatus(), Manager.from(slot.getManager()));
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
