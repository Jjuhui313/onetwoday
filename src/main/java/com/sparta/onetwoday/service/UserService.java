package com.sparta.onetwoday.service;

import com.sparta.onetwoday.dto.CustomException;
import com.sparta.onetwoday.dto.LoginRequestDto;
import com.sparta.onetwoday.dto.Message;
import com.sparta.onetwoday.dto.SignupRequestDto;
import com.sparta.onetwoday.entity.User;
import com.sparta.onetwoday.entity.UserRoleEnum;
import com.sparta.onetwoday.jwt.JwtUtil;
import com.sparta.onetwoday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static com.sparta.onetwoday.entity.ExceptionMessage.*;
import static com.sparta.onetwoday.entity.SuccessMessage.LOGIN_SUCCESS;
import static com.sparta.onetwoday.entity.SuccessMessage.SIGN_UP_SUCCESS;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;
    private static final String ADMIN_TOKEN = "AAABnvxRVklrnYxKZ0aHgTBcXukeZygoC";

    @Transactional
    public ResponseEntity<Message> signup(SignupRequestDto signupRequestDto) {
        String username = signupRequestDto.getUsername();
        String password = passwordEncoder.encode(signupRequestDto.getPassword());
        String nickname = signupRequestDto.getNickname();

        // 회원 중복 확인
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isPresent()) {
            throw new CustomException(DUPLICATE_USER);
        }
        found = userRepository.findByNickname(nickname);
        if (found.isPresent()) {
            throw new CustomException(DUPLICATE_NICKNAME);
        }

        // 사용자 ROLE 확인
        UserRoleEnum role = UserRoleEnum.USER;
        if (signupRequestDto.isAdmin()) {
            if (!signupRequestDto.getAdminToken().equals(ADMIN_TOKEN)) {
                throw new CustomException(UNAUTHORIZED_ADMIN);
            }
            role = UserRoleEnum.ADMIN;
        }

        //닉네임이 공백포함인지 확인
        if(nickname.replaceAll(" ","").equals("")) {
            throw new CustomException(NICKNAME_WITH_SPACES);
        }

        User user = new User(username, nickname, password, role);
        userRepository.save(user);

        return Message.toResponseEntity(SIGN_UP_SUCCESS);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Message> login(LoginRequestDto loginRequestDto, HttpServletResponse response) {
        String username = loginRequestDto.getUsername();
        String password = loginRequestDto.getPassword();

        // 사용자 확인
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new CustomException(MEMBER_NOT_FOUND)
        );
        // 비밀번호 확인
        if(!passwordEncoder.matches(password, user.getPassword())){
            throw new CustomException(MEMBER_NOT_FOUND);
        }

        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, jwtUtil.createToken(user.getUsername(), user.getRole()));

        return Message.toResponseEntity(LOGIN_SUCCESS, jwtUtil.createToken(user.getUsername(), user.getRole()));
    }

}
