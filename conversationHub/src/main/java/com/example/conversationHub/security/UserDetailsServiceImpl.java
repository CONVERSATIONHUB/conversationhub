package com.example.conversationHub.security;

import com.example.conversationHub.domain.AppUser;
import com.example.conversationHub.repository.AppUserRepository;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository userRepository;

    public UserDetailsServiceImpl(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AppUser> found = userRepository.findByPhone(username);
        if (found.isEmpty()) {
            found = userRepository.findByEmail(username);
        }

        AppUser user = found.orElseThrow(() ->
                new UsernameNotFoundException("No account found for: " + username));

        String authority = "ROLE_" + user.getRole().getName();

        return User.withUsername(username)
                .password(user.getPasswordHash())
                .authorities(AuthorityUtils.createAuthorityList(authority))
                .disabled(!user.isVerified())
                .build();
    }
}
