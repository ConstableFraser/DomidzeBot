package org.shvedchikov.domidzebot.util;

import org.shvedchikov.domidzebot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserUtils {

    @Autowired
    private UserRepository userRepository;

    public boolean isCurrentUser(Long userId) {
        var userName = userRepository.findById(userId).orElseThrow().getFirstName();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return userName.equals(authentication.getName());
    }
}
