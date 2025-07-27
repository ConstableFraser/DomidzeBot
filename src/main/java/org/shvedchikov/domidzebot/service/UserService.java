package org.shvedchikov.domidzebot.service;

import jakarta.validation.Valid;
import org.shvedchikov.domidzebot.dto.user.UserCreateDTO;
import org.shvedchikov.domidzebot.dto.user.UserDTO;
import org.shvedchikov.domidzebot.dto.user.UserUpdateDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.mapper.UserMapper;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    public List<UserDTO> getAll() {
        var users = userRepository.findAll();
        return users.stream()
                .map(userMapper::map)
                .toList();
    }

    public UserDTO create(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        var user = userMapper.map(userCreateDTO);
        userRepository.save(user);

        return userMapper.map(user);
    }

    //@PreAuthorize("@userUtils.isCurrentUser(#id)")
    public UserDTO update(@RequestBody UserUpdateDTO userData, @PathVariable Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + id + " not found"));
        userMapper.update(userData, user);
        user = userRepository.save(user);

        return userMapper.map(user);
    }

    public UserDTO show(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + id + " not found"));
        return userMapper.map(user);
    }

    @PreAuthorize("@userUtils.isCurrentUser(#id)")
    public void destroy(@PathVariable Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id: " + id + " not found"));
        userRepository.deleteById(id);
    }
}
