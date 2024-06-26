package org.changppo.account.security.sign;

import lombok.RequiredArgsConstructor;
import org.changppo.account.entity.member.Member;
import org.changppo.account.entity.member.Role;
import org.changppo.account.repository.member.MemberRepository;
import org.changppo.account.repository.member.RoleRepository;
import org.changppo.account.response.exception.member.RoleNotFoundException;
import org.changppo.account.response.exception.oauth2.AdminBannedException;
import org.changppo.account.response.exception.oauth2.MemberDeletionRequestedException;
import org.changppo.account.security.sign.response.OAuth2Response;
import org.changppo.account.security.sign.response.OAuth2ResponseFactory;
import org.changppo.account.type.RoleType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = OAuth2ResponseFactory.getOAuth2Response(registrationId, oAuth2User.getAttributes());
        String name = oAuth2Response.getProvider() + "_" + oAuth2Response.getProviderId();

        Member member = memberRepository.findByNameWithRoles(name)
                .map(existingMember -> {
                    if (existingMember.isDeletionRequested()) {
                        throw new MemberDeletionRequestedException("Member deletion requested");
                    }
                    else if (existingMember.isAdminBanned()) {
                        throw new AdminBannedException("Admin banned");
                    }
                    existingMember.updateInfo(oAuth2Response.getName(), oAuth2Response.getProfileImage());
                    return existingMember;
                })
                .orElseGet(() -> {
                    Role freeRole = roleRepository.findByRoleType(RoleType.ROLE_FREE).orElseThrow(RoleNotFoundException::new);
                    return memberRepository.save(
                        Member.builder()
                                .name(name)
                                .username(oAuth2Response.getName())
                                .profileImage(oAuth2Response.getProfileImage())
                                .role(freeRole)
                                .build());
                });

        return new CustomOAuth2UserDetails(member.getId(), name, null, Collections.singleton(new SimpleGrantedAuthority(member.getRole().getRoleType().name())));
    }
}
