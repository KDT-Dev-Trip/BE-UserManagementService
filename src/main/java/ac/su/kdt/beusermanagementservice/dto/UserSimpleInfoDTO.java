package ac.su.kdt.beusermanagementservice.dto;

import ac.su.kdt.beusermanagementservice.entity.User;

// 대시보드에 포함될 간단한 사용자 정보
public record UserSimpleInfoDTO(Long id, String name, String profileImageUrl) {
    public static UserSimpleInfoDTO from(User user) {
        return new UserSimpleInfoDTO(user.getId(), user.getName(), user.getProfileImageUrl());
    }
}