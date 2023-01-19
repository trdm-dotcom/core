package com.homer.core.services;

import com.ea.async.Async;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.exceptions.InvalidValueException;
import com.homer.core.configurations.AppConf;
import com.homer.core.constants.Constants;
import com.homer.core.model.FirebaseType;
import com.homer.core.model.db.Booking;
import com.homer.core.model.db.Post;
import com.homer.core.model.dto.BookingDTO;
import com.homer.core.model.request.*;
import com.homer.core.model.response.UserInfo;
import com.homer.core.repository.BookingRepository;
import com.homer.core.repository.PostRepository;
import com.homer.core.utils.Utils;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepository;

    private final PostRepository postRepository;

    private final AppConf appConf;

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            PostRepository postRepository,
            AppConf appConf
    ) {
        this.bookingRepository = bookingRepository;
        this.postRepository = postRepository;
        this.appConf = appConf;
    }

    public Object createBooking(CreateBookingRequest request, String msgId) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        log.info("{} create booking {}", msgId, request);
        String userId = request.getHeaders().getToken().getUserData().getId();
        UserInfo userInfo = Utils.getUserInfo(msgId, userId);
        if (!userInfo.getIsVerified()) {
            throw new GeneralException(Constants.USER_HADNT_BEEN_VERIFIED);
        }
        if (StringUtil.isNullOrEmpty(request.getHash())) {
            throw new InvalidValueException("hash");
        }
        if (request.getFromTime() == null) {
            throw new InvalidValueException("fromTime");
        }
        if (request.getToTime() == null) {
            throw new InvalidValueException("toTime");
        }
        if (request.getPostId() == null) {
            throw new InvalidValueException("postId");
        }
        if (request.getFromTime().isBefore(LocalDateTime.now())) {
            throw new GeneralException(Constants.FROM_TIME_MUST_BE_AFTER_OR_EQUAL_TO_CURRENT_TIME);
        }
        if (request.getToTime().isBefore(LocalDateTime.now())) {
            throw new GeneralException(Constants.TO_DATE_MUST_BE_AFTER_OR_EQUAL_TO_CURRENT_TIME);
        }
        if (request.getToTime().isBefore(request.getFromTime())) {
            throw new GeneralException(Constants.FROM_TIME_MUST_BE_BEFORE_OR_EQUAL_TO_TO_TIME);
        }
        Utils.validate(request.getHash(), "CREATE", LocalDateTime.now());
        Post post = postRepository.findById(request.getPostId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (!post.getIsPublic()) {
            throw new GeneralException(Constants.CREATE_FAILED);
        }
        if (post.getUserId().equals(request.getHeaders().getToken().getUserData().getId())) {
            throw new GeneralException(Constants.CREATE_FAILED);
        }
        Optional<Booking> opt = this.bookingRepository.findByUserIdAndPostIdAndActiveAndToTimeLessThan(userId, post.getId(), true, LocalDateTime.now());
        if (opt.isPresent()) {
            throw new GeneralException(Constants.ALREADY_EXISTS);
        }
        Booking booking = new Booking();
        booking.setActive(true);
        booking.setToTime(request.getToTime());
        booking.setFromTime(request.getFromTime());
        booking.setUserId(userId);
        booking.setUserIdSideB(post.getUserId());
        booking.setPost(post);
        this.bookingRepository.save(booking);
        Utils.sendNotification(msgId, post.getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<String, Object>() {{
            put("message", Constants.CREATE_SUCCESS);
        }};
    }

    public List<BookingDTO> getBooking(GetBookingRequest request, String msgId) {
        log.info("{} get booking {}", msgId, request);
        if (request.getSide() == null) request.setSide(false);
        int offset = request.getOffset() == null ? Constants.DEFAULT_OFFSET : Math.max(request.getOffset(), Constants.DEFAULT_OFFSET);
        int fetchCount = request.getFetchCount() == null ? Constants.DEFAULT_FETCH_COUNT : Math.max(request.getFetchCount(), Constants.DEFAULT_FETCH_COUNT);
        return this.bookingRepository.findAll(request.getSide(),
                        request.getHeaders().getToken().getUserData().getId(),
                        request.getFromTime(),
                        request.getToTime(),
                        PageRequest.of(offset, fetchCount))
                .stream()
                .map(b -> {
                    BookingDTO bookingDTO = new BookingDTO();
                    UserInfo userInfo;
                    if (request.getSide()) {
                        userInfo = Utils.getUserInfo(msgId, b.getUserId());
                    } else {
                        userInfo = Utils.getUserInfo(msgId, b.getUserIdSideB());
                    }
                    bookingDTO.setName(userInfo.getFullname());
                    bookingDTO.setPhoneNumber(userInfo.getUsername());
                    bookingDTO.setFromTime(b.getFromTime());
                    bookingDTO.setToTime(b.getToTime());
                    return bookingDTO;
                })
                .collect(Collectors.toList());
    }

    public Object modifyBooking(UpdateBookingRequest request, String msgId) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        log.info("{} modify booking {}", msgId, request);
        Booking booking = this.bookingRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (StringUtil.isNullOrEmpty(request.getHash())) {
            throw new InvalidValueException("hash");
        }
        if (!booking.getUserId().equals(request.getHeaders().getToken().getUserData().getId())) {
            throw new GeneralException(Constants.MODIFY_FAILED);
        }
        if (request.getFromTime().isBefore(LocalDateTime.now())) {
            throw new GeneralException(Constants.FROM_TIME_MUST_BE_AFTER_OR_EQUAL_TO_CURRENT_TIME);
        }
        if (request.getToTime().isBefore(LocalDateTime.now())) {
            throw new GeneralException(Constants.TO_DATE_MUST_BE_AFTER_OR_EQUAL_TO_CURRENT_TIME);
        }
        if (request.getToTime().isBefore(request.getFromTime())) {
            throw new GeneralException(Constants.FROM_TIME_MUST_BE_BEFORE_OR_EQUAL_TO_TO_TIME);
        }
        if (booking.getFromTime().plus(appConf.getTimeModify(), ChronoUnit.SECONDS).isAfter(LocalDateTime.now())) {
            throw new GeneralException(Constants.MODIFY_FAILED);
        }
        Utils.validate(request.getHash(), "UPDATE", LocalDateTime.now());
        booking.setToTime(request.getToTime());
        booking.setFromTime(request.getFromTime());
        Utils.sendNotification(msgId, booking.getPost().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<String, Object>() {{
            put("message", Constants.MODIFY_SUCCESS);
        }};
    }

    public Object deleteBooking(UpdateBookingRequest request, String msgId) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        log.info("{} delete booking {}", msgId, request);
        Booking booking = this.bookingRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (StringUtil.isNullOrEmpty(request.getReason())) {
            throw new InvalidValueException("reason");
        }
        if (booking.getFromTime().plus(appConf.getTimeModify(), ChronoUnit.SECONDS).isAfter(LocalDateTime.now())) {
            throw new GeneralException(Constants.DELETE_FAILED);
        }
        if (!booking.getUserId().equals(request.getHeaders().getToken().getUserData().getId())) {
            throw new GeneralException(Constants.DELETE_FAILED);
        }
        Utils.validate(request.getHash(), "DELETE", LocalDateTime.now());
        this.bookingRepository.delete(booking);
        Utils.sendNotification(msgId, booking.getPost().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<String, Object>() {{
            put("message", Constants.DELETE_SUCCESS);
        }};
    }

    public Object rejectBooking(UpdateBookingRequest request, String msgId) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        log.info("{} reject booking {}", msgId, request);
        Booking booking = this.bookingRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (StringUtil.isNullOrEmpty(request.getHash())) {
            throw new InvalidValueException("hash");
        }

        if (StringUtil.isNullOrEmpty(request.getReason())) {
            throw new InvalidValueException("reason");
        }
        if (booking.getFromTime().plus(appConf.getTimeModify(), ChronoUnit.SECONDS).isAfter(LocalDateTime.now())) {
            throw new GeneralException(Constants.DELETE_FAILED);
        }
        if (!booking.getUserIdSideB().equals(request.getHeaders().getToken().getUserData().getId())) {
            throw new GeneralException(Constants.REJECT_FAILED);
        }
        Utils.validate(request.getHash(), "REJECT", LocalDateTime.now());
        booking.setActive(false);
        booking.setReason(request.getReason());
        this.bookingRepository.save(booking);
        Utils.sendNotification(msgId, booking.getPost().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<String, Object>() {{
            put("message", Constants.REJECT_SUCCESS);
        }};
    }

    public void internalRejectBooking(Post post, String msgId) throws IOException {
        log.info("{} internal reject booking {}", msgId, post);
        List<Booking> bookings = this.bookingRepository.findByPost(post);
        for (Booking booking : bookings) {
            booking.setActive(false);
            booking.setReason("INTERNAL_REJECT");
            this.bookingRepository.save(booking);
            Utils.sendNotification(msgId, booking.getPost().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        }
    }

}
