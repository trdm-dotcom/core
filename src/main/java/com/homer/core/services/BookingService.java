package com.homer.core.services;

import com.homer.core.model.dto.BookingDTO;
import com.homer.core.model.request.CreateBookingRequest;
import com.homer.core.model.request.GetBookingRequest;
import com.homer.core.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class BookingService {
    private final BookingRepository bookingRepository;

    public BookingService(
            BookingRepository bookingRepository
    ) {
        this.bookingRepository = bookingRepository;
    }

    public Object createBooking(CreateBookingRequest request, String msgId){
        log.info("{} create booking {}", msgId, request);
        return new HashMap<>();
    }

    public List<BookingDTO> getBooking(GetBookingRequest request, String msgId){
        log.info("{} get booking {}", msgId, request);
        List<BookingDTO> list = new ArrayList<>();
        return list;
    }
}
