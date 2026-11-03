package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// 관리자 대여 목록 한 줄. 미반납 연락용으로 학생 이메일·전화번호를 같이 준다
public record AdminRentalResponse(
        Long rentId,
        String studentId,
        String studentName,
        String email,
        String phoneNum,
        String department,
        Long itemId,
        String itemName,
        Integer itemNumber,
        Long slotId,
        String managerName,
        LocalDate pickupDate,
        LocalTime pickupStart,
        LocalTime pickupEnd,
        LocalDateTime dueAt,
        String purpose,
        RentStatus status,
        LocalDateTime returnedAt,
        boolean overdue,
        LocalDateTime createdAt
) {
    public static AdminRentalResponse of(Rent rent, LocalDateTime now) {
        Student student = rent.getStudent();
        return new AdminRentalResponse(rent.getId(), student.getId(), student.getName(), student.getEmail(),
                student.getPhoneNum(), student.getDepartment(), rent.getItem().getId(), rent.getItem().getName(),
                rent.getItem().getItemNumber(), rent.getSlot().getId(), rent.getSlot().getManager().getName(),
                rent.getPickupDate(), rent.getSlot().getStartTime(), rent.getSlot().getEndTime(), rent.getDueAt(),
                rent.getPurpose(), rent.getStatus(), rent.getReturnedAt(), rent.isOverdue(now), rent.getCreatedAt());
    }
}
