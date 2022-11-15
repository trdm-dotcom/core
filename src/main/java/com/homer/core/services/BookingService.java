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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepository;

    private final PostRepository postRepository;

    private final AppConf appConf;

    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            PostRepository postRepository,
            AppConf appConf,
            KafkaProducerService kafkaProducerService
    ) {
        this.bookingRepository = bookingRepository;
        this.postRepository = postRepository;
        this.appConf = appConf;
        this.kafkaProducerService = kafkaProducerService;
    }

    public Object createBooking(CreateBookingRequest request, String msgId) throws IOException {
        log.info("{} create booking {}", msgId, request);
        UserInfo userInfo = Async.await(Utils.getUserInfo(msgId, request.getHeaders().getToken().getUserData().getUserId()));
        if (!userInfo.getIsVerified()) {
            throw new GeneralException(Constants.USER_HADNT_BEEN_VERIFIED);
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
        Post post = postRepository.findById(request.getPostId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (!post.getIsPublic()) {
            throw new GeneralException("OBJECT_NOT_FOUND");
        }
        if (post.getUserId().equals(request.getHeaders().getToken().getUserData().getUserId())) {
            throw new GeneralException(Constants.CREATE_FAILED);
        }
        Booking booking = new Booking();
        booking.setActive(true);
        booking.setToTime(request.getToTime());
        booking.setFromTime(request.getFromTime());
        booking.setUserId(request.getHeaders().getToken().getUserData().getUserId());
        booking.setUserIdSideB(post.getUserId());
        booking.setPost(post);
        this.bookingRepository.save(booking);
        this.sendNotification(post.getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<>();
    }

    public List<BookingDTO> getBooking(GetBookingRequest request, String msgId) {
        log.info("{} get booking {}", msgId, request);
        if (request.getSide() == null) request.setSide(false);
        int offset = request.getOffset() == null ? Constants.DEFAULT_OFFSET : Math.max(request.getOffset(), Constants.DEFAULT_OFFSET);
        int fetchCount = request.getFetchCount() == null ? Constants.DEFAULT_FETCH_COUNT : Math.max(request.getFetchCount(), Constants.DEFAULT_FETCH_COUNT);
        return this.bookingRepository.findAll(request.getSide(),
                        request.getHeaders().getToken().getUserData().getUserId(),
                        request.getFromTime(),
                        request.getToTime(),
                        PageRequest.of(offset, fetchCount))
                .stream()
                .map(b -> {
                    BookingDTO bookingDTO = new BookingDTO();
                    UserInfo userInfo;
                    if (request.getSide()) {
                        userInfo = Utils.getUserInfo(b.getUserId());
                    } else {
                        userInfo = Utils.getUserInfo(b.getUserIdSideB());
                    }
                    bookingDTO.setName(userInfo.getFullname());
                    bookingDTO.setPhoneNumber(userInfo.getUsername());
                    bookingDTO.setFromTime(b.getFromTime());
                    bookingDTO.setToTime(b.getToTime());
                    return bookingDTO;
                })
                .collect(Collectors.toList());
    }

    public Object modifyBooking(UpdateBookingRequest request, String msgId) throws IOException {
        log.info("{} modify booking {}", msgId, request);
        Booking booking = this.bookingRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
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
        if (!booking.getUserId().equals(request.getHeaders().getToken().getUserData().getUserId())) {
            throw new GeneralException(Constants.MODIFY_FAILED);
        }
        booking.setToTime(request.getToTime());
        booking.setFromTime(request.getFromTime());
        this.sendNotification(booking.getPost().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<>();
    }

    public Object deleteBooking(UpdateBookingRequest request, String msgId) throws IOException {
        log.info("{} delete booking {}", msgId, request);
        Booking booking = this.bookingRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (StringUtil.isNullOrEmpty(request.getReason())) {
            throw new InvalidValueException("reason");
        }
        if (booking.getFromTime().plus(appConf.getTimeModify(), ChronoUnit.SECONDS).isAfter(LocalDateTime.now())) {
            throw new GeneralException(Constants.DELETE_FAILED);
        }
        if (!booking.getUserId().equals(request.getHeaders().getToken().getUserData().getUserId())) {
            throw new GeneralException(Constants.DELETE_FAILED);
        }
        this.bookingRepository.delete(booking);
        this.sendNotification(booking.getPost().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<>();
    }

    public Object rejectBooking(UpdateBookingRequest request, String msgId) throws IOException {
        log.info("{} reject booking {}", msgId, request);
        Booking booking = this.bookingRepository.findById(request.getId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (StringUtil.isNullOrEmpty(request.getReason())) {
            throw new InvalidValueException("reason");
        }
        if (booking.getFromTime().plus(appConf.getTimeModify(), ChronoUnit.SECONDS).isAfter(LocalDateTime.now())) {
            throw new GeneralException(Constants.DELETE_FAILED);
        }
        if (!booking.getUserIdSideB().equals(request.getHeaders().getToken().getUserData().getUserId())) {
            throw new GeneralException(Constants.REJECT_FAILED);
        }
        booking.setActive(false);
        booking.setReason(request.getReason());
        this.bookingRepository.save(booking);
        this.sendNotification(booking.getPost().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<>();
    }

    public void internalRejectBooking(InternalRejectBookingRequest request, String msgId) throws IOException {
        log.info("{} internal reject booking {}", msgId, request);
        List<Booking> bookings = this.bookingRepository.findByPostId(request.getId());
        for (Booking booking : bookings) {
            booking.setActive(false);
            booking.setReason("INTERNAL_REJECT");
            this.bookingRepository.save(booking);
            this.sendNotification(booking.getPost().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        }
    }

    private void sendNotification(String userId, String titile, String content, String template, FirebaseType type, String condition) throws IOException {
        UserInfo userInfo = Utils.getUserInfo(userId);
        PushNotificationRequest request = new PushNotificationRequest();
        request.setUserId(userId);
        request.setTitle(titile);
        request.setContent(content);
        request.setTemplate(template);
        request.setIsSave(true);
        request.setType(type);
        if (type.equals(FirebaseType.TOKEN)) {
            request.setToken(userInfo.getDeviceToken());
        } else {
            request.setCondition(condition);
        }
        this.kafkaProducerService.sendMessage(appConf.getTopics().getPushNotification(), "", request);
    }
}
