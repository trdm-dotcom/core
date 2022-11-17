package com.homer.core.consumers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.common.kafka.producers.KafkaRequestHandler;
import com.homer.core.common.model.Message;
import com.homer.core.configurations.AppConf;
import com.homer.core.model.request.*;
import com.homer.core.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rx.Observable;

@Service
@Slf4j
public class RequestHandler extends KafkaRequestHandler {
    private final ObjectMapper objectMapper;
    private final AddressService addressService;
    private final PostService postService;
    private final VnPayService vnPayService;
    private final WatchlistService watchlistService;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;

    @Autowired
    public RequestHandler(
            ObjectMapper objectMapper,
            AppConf appConf,
            AddressService addressService,
            PostService postService,
            VnPayService vnPayService,
            WatchlistService watchlistService,
            BookingService bookingService,
            InvoiceService invoiceService
    ) {
        super(objectMapper, appConf.getKafkaBootstraps(), appConf.getClusterId(), appConf.getMaxThread());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        objectMapper.coercionConfigFor(LogicalType.Enum).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        this.objectMapper = objectMapper;
        this.addressService = addressService;
        this.postService = postService;
        this.vnPayService = vnPayService;
        this.watchlistService = watchlistService;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
    }

    @Override
    protected Object handle(Message message) {
        if (message == null || message.getData() == null) {
            log.error("Invalid data");
            return true;
        }
        try {
            log.info("message: {}", message);
            switch (message.getUri()) {
                case "get:/api/v1/core/cities":
                    return this.addressService.getCities(message.getTransactionId());

                case "get:/api/v1/core/districts":
                    AddressRequest districtRequest = Message.getData(this.objectMapper, message, AddressRequest.class);
                    return this.addressService.getDistrictsByCity(districtRequest, message.getTransactionId());

                case "get:/api/v1/core/communes":
                    AddressRequest communeRequest = Message.getData(this.objectMapper, message, AddressRequest.class);
                    return this.addressService.getCommuneByDistrict(communeRequest, message.getTransactionId());

                case "post:/api/v1/core/post":
                    PostRequest createPostRequest = Message.getData(this.objectMapper, message, PostRequest.class);
                    return this.postService.createNewPost(createPostRequest, message.getTransactionId());

                case "put:/api/v1/core/post":
                    PostRequest updatePostRequest = Message.getData(this.objectMapper, message, PostRequest.class);
                    return this.postService.updatePost(updatePostRequest, message.getTransactionId());

                case "get:/api/v1/core/post":
                    FilterPostRequest filterPostRequest = Message.getData(this.objectMapper, message, FilterPostRequest.class);
                    return this.postService.getPost(filterPostRequest, message.getMessageId());

                case "delete:/api/v1/core/post":
                    PostRequest deletePostRequest = Message.getData(this.objectMapper, message, PostRequest.class);
                    return this.postService.deletePost(deletePostRequest, message.getTransactionId());

                case "get:/api/v1/core/post/detail":
                    PostDetailRequest postDetailRequest = Message.getData(this.objectMapper, message, PostDetailRequest.class);
                    return this.postService.getPostDetail(postDetailRequest, message.getTransactionId());

                case "put:/api/v1/core/favorite/watchlist":
                    AddWatchListRequest addWatchListRequest = Message.getData(this.objectMapper, message, AddWatchListRequest.class);
                    return this.watchlistService.addWatchList(addWatchListRequest, message.getTransactionId());

                case "delete:/api/v1/core/favorite/watchlist":
                    DeleteWatchListRequest deleteWatchListRequest = Message.getData(this.objectMapper, message, DeleteWatchListRequest.class);
                    return this.watchlistService.deleteWatchList(deleteWatchListRequest, message.getTransactionId());

                case "get:/api/v1/core/favorite/watchlist":
                    GetWatchlistRequest getWatchlistRequest = Message.getData(this.objectMapper, message, GetWatchlistRequest.class);
                    return this.watchlistService.getWatchList(getWatchlistRequest, message.getTransactionId());

                case "post:/api/v1/core/booking":
                    CreateBookingRequest createBookingRequest = Message.getData(this.objectMapper, message, CreateBookingRequest.class);
                    return this.bookingService.createBooking(createBookingRequest, message.getTransactionId());

                case "get:/api/v1/core/booking":
                    GetBookingRequest getBookingRequest = Message.getData(this.objectMapper, message, GetBookingRequest.class);
                    return this.bookingService.getBooking(getBookingRequest, message.getTransactionId());

                case "put:/api/v1/core/booking":
                    UpdateBookingRequest modifyBookingRequest = Message.getData(this.objectMapper, message, UpdateBookingRequest.class);
                    return this.bookingService.modifyBooking(modifyBookingRequest, message.getTransactionId());

                case "delete:/api/v1/core/booking":
                    UpdateBookingRequest deleteBookingRequest = Message.getData(this.objectMapper, message, UpdateBookingRequest.class);
                    return this.bookingService.deleteBooking(deleteBookingRequest, message.getTransactionId());

                case "put:/api/v1/core/booking/reject":
                    UpdateBookingRequest rejectBookingRequest = Message.getData(this.objectMapper, message, UpdateBookingRequest.class);
                    return this.bookingService.rejectBooking(rejectBookingRequest, message.getTransactionId());

                case "post:/api/v1/core/invoice":
                    RequestingRepairRequest requestingRepairRequest = Message.getData(this.objectMapper, message, RequestingRepairRequest.class);
                    this.invoiceService.createNewInvoice(requestingRepairRequest, message.getTransactionId());

                case "post:/api/v1/core/vnpay/payment/url":
                    CustomerPaymentUrlRequest customerPaymentUrlRequest = Message.getData(this.objectMapper, message, CustomerPaymentUrlRequest.class);
                    return this.vnPayService.createCustomerPaymentUrl(customerPaymentUrlRequest, message.getTransactionId());

                case "get:/api/v1/core/vnpay/payment/ipn":
                    VnPayRequest vnPayCustomerPaymentRequest = Message.getData(this.objectMapper, message, VnPayRequest.class);
                    return this.vnPayService.responseCustomerPayment(vnPayCustomerPaymentRequest, message.getTransactionId());

                case "get:/api/v1/core/vnpay/deposit/ipn":
                    VnPayRequest vnPayRepairerDepositRequest = Message.getData(this.objectMapper, message, VnPayRequest.class);
                    return this.vnPayService.responseRepairerDeposit(vnPayRepairerDepositRequest, message.getTransactionId());
            }
            return true;
        } catch (IllegalArgumentException e) {
            return Observable.error(new GeneralException(e.getMessage()));
        } catch (Exception e) {
            log.error("Error: ", e);
            return Observable.error(e);
        }
    }
}
