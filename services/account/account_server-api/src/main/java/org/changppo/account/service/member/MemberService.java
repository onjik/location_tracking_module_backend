package org.changppo.account.service.member;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.changppo.account.entity.member.Member;
import org.changppo.account.repository.apikey.ApiKeyRepository;
import org.changppo.account.repository.member.MemberRepository;
import org.changppo.account.response.exception.member.MemberNotFoundException;
import org.changppo.account.security.sign.CustomOAuth2UserDetails;
import org.changppo.account.service.dto.member.MemberDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final SessionRegistry sessionRegistry;

    @PreAuthorize("@memberAccessEvaluator.check(#id)")
    public MemberDto read(@Param("id")Long id) {
        return memberRepository.findDtoById(id).orElseThrow(MemberNotFoundException::new);
    }

    public Page<MemberDto> readList(Pageable pageable) {
        return memberRepository.findAllDtos(pageable);
    }

    @Transactional
    @PreAuthorize("@memberAccessEvaluator.check(#id) and @memberNotPaymentFailureStatusEvaluator.check(#id)")
    public void requestDelete(@Param("id")Long id, HttpServletRequest request, HttpServletResponse response) {
        Member member = memberRepository.findById(id).orElseThrow(MemberNotFoundException::new);
        member.requestDeletion(LocalDateTime.now());
        apiKeyRepository.requestApiKeyDeletion(id, LocalDateTime.now());
        deleteSession(request);
        deleteCookie(response);
    }

    public void deleteSession(HttpServletRequest request){
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public void deleteCookie(HttpServletResponse response){
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        // refreshCookie.setSecure(true);
        response.addCookie(cookie);
    }

    @Transactional
    public void cancelDelete(@Param("id")Long id) {
        Member member = memberRepository.findById(id).orElseThrow(MemberNotFoundException::new);
        member.cancelDeletionRequest();
        apiKeyRepository.cancelApiKeyDeletionRequest(id);
    }


    @Transactional
    public void ban(@Param("id")Long id) {
        Member member = memberRepository.findById(id).orElseThrow(MemberNotFoundException::new);
        member.banByAdmin(LocalDateTime.now());
        apiKeyRepository.banApiKeysByAdmin(id, LocalDateTime.now());
        deleteMemberSessions(member.getName());
    }


    private void deleteMemberSessions(String name) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof CustomOAuth2UserDetails)
                .map(principal -> (CustomOAuth2UserDetails) principal)
                .filter(userDetails -> userDetails.getName().equals(name))
                .flatMap(userDetails -> sessionRegistry.getAllSessions(userDetails, false).stream())
                .forEach(SessionInformation::expireNow);
    }

    @Transactional
    public void unban(@Param("id")Long id) {
        Member member = memberRepository.findById(id).orElseThrow(MemberNotFoundException::new);
        member.unbanByAdmin();
        apiKeyRepository.unbanApiKeysByAdmin(id);
    }
}
