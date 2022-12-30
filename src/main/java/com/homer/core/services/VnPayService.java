package com.homer.core.services;

import com.ea.async.Async;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.configurations.AppConf;
import com.homer.core.constants.Constants;
import com.homer.core.model.*;
import com.homer.core.model.db.Invoice;
import com.homer.core.model.db.Transaction;
import com.homer.core.model.db.TransactionHistory;
import com.homer.core.model.request.CustomerPaymentUrlRequest;
import com.homer.core.model.request.VnPayRequest;
import com.homer.core.model.response.UserInfo;
import com.homer.core.repository.InvoiceRepository;
import com.homer.core.repository.TransactionHistoryRepository;
import com.homer.core.repository.TransactionRepository;
import com.homer.core.utils.DateFormatUtil;
import com.homer.core.utils.Utils;
import com.homer.core.utils.vnpay.crypto.VnPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.homer.core.constants.VnPayConstants.*;

@Service
@Slf4j
public class VnPayService {
    private final AppConf appConf;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final CallApiService callApiService;

    @Autowired
    public VnPayService(
            AppConf appConf,
            InvoiceRepository invoiceRepository,
            TransactionRepository transactionRepository,
            TransactionHistoryRepository transactionHistoryRepository,
            CallApiService callApiService
    ) {
        this.appConf = appConf;
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.callApiService = callApiService;
    }

    public Map<String, String> createCustomerPaymentUrl(CustomerPaymentUrlRequest request, String msgId) throws Exception {
        log.info("{} create customer payment url {}", msgId, request);
        request.validate();
        UserInfo userInfo = Async.await(Utils.getUserInfo(msgId, request.getHeaders().getToken().getUserData().getUserId()));
        if (!userInfo.getIsVerified()) {
            throw new GeneralException(Constants.USER_HADNT_BEEN_VERIFIED);
        }
        Invoice invoice = this.invoiceRepository.findById(request.getRequestCode()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        invoice.setStatus(InvoiceStatus.PAYMENT_WAITING);
        this.invoiceRepository.save(invoice);
        Map<String, Object> vnpParams = buildVnpParams(
                request.getOrderInfo(),
                invoice.getPrice() * appConf.getVnPay().getVnPayAmountRate(),
                request.getSourceIp(),
                request.getRequestCode(),
                request.getHeaders().getAcceptLanguage(),
                appConf.getVnPay().getPaymentInfo().getReturnUrl(),
                appConf.getVnPay().getPaymentInfo().getTmnCode());
        String url = this.buildPaymentUrl(vnpParams, appConf.getVnPay().getPaymentInfo().getSecureHash());
        return new HashMap<String, String>() {{
            put("url", url);
        }};
    }

    private void repairer(Transaction transaction, Invoice invoice, String orderInfo, String userId, String msgId) throws Exception {
        log.info("{} repairer deposit", msgId);
        String txnRef = String.format("%s_%s", userId, VnPayUtil.getRandomNumber(8));
        invoice.setStatus(InvoiceStatus.REPAIRER_WAITING);
        this.invoiceRepository.save(invoice);
        Map<String, Object> vnpParams = buildVnpParams(
                orderInfo,
                invoice.getPrice() * appConf.getVnPay().getVnPayAmountRate(),
                InetAddress.getLocalHost().getHostAddress(),
                txnRef,
                "vn",
                appConf.getVnPay().getDepositInfo().getReturnUrl(),
                appConf.getVnPay().getDepositInfo().getTmnCode());
        String url = this.buildPaymentUrl(vnpParams, appConf.getVnPay().getDepositInfo().getSecureHash());
        Map<Object, Object> reponse = Async.await(this.callApiService.get(url, null, new ParameterizedTypeReference<HashMap<Object, Object>>() {
        }));
        transaction.setStatus(TransactionStatus.REPAIRER_WAITING);
        this.transactionRepository.save(transaction);
    }

    private void refund(Transaction transaction) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String userId = transaction.getInvoice().getUserId();
        Map<String, Object> vnpParams = new HashMap<>();
        vnpParams.put(VNP_VERSION, appConf.getVnPay().getVnpVersion());
        vnpParams.put(VNP_COMMAND, "refund");
        vnpParams.put(VNP_TMN_CODE, appConf.getVnPay().getDepositInfo().getTmnCode());
        vnpParams.put(VNP_AMOUNT, transaction.getInvoice().getPrice() * appConf.getVnPay().getVnPayAmountRate());
        vnpParams.put(VNP_TNX_REF, transaction.getRequestCode());
        vnpParams.put(VNP_ORDER_INFO, "");
        vnpParams.put(VNP_TRANS_DATE, DateFormatUtil.toString(now, appConf.getVnPay().getDatePattern()));
        vnpParams.put(VNP_IP_ADDR, InetAddress.getLocalHost().getHostAddress());
        vnpParams.put(VNP_TRANSACTION_TYPE, "");
        vnpParams.put(VNP_CREATE_DATE, DateFormatUtil.toString(now, appConf.getVnPay().getDatePattern()));
        String url = this.buildPaymentUrl(vnpParams, appConf.getVnPay().getDepositInfo().getSecureHash());
        Map<Object, Object> reponse = Async.await(this.callApiService.get(url, null, new ParameterizedTypeReference<HashMap<Object, Object>>() {
        }));
        Utils.sendNotification(userId, "", "", "", FirebaseType.TOKEN, null);
    }

    private Map<String, Object> buildVnpParams(
            String vnpOrderInfo,
            Double amount,
            String vnpIpAddress,
            String tnxRef,
            String vnpLocale,
            String returnUrl,
            String tmnCode

    ) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> vnpParams = new HashMap<>();
        vnpParams.put(VNP_VERSION, appConf.getVnPay().getVnpVersion());
        vnpParams.put(VNP_COMMAND, appConf.getVnPay().getVnpCommand());
        vnpParams.put(VNP_AMOUNT, amount);
        vnpParams.put(VNP_CURR_CODE, appConf.getVnPay().getVnpCurrCode());
        vnpParams.put(VNP_TNX_REF, tnxRef);
        vnpParams.put(VNP_ORDER_INFO, vnpOrderInfo);
        vnpParams.put(VNP_LOCALE, vnpLocale);
        vnpParams.put(VNP_RETURN_URL, returnUrl);
        vnpParams.put(VNP_TMN_CODE, tmnCode);
        vnpParams.put(VNP_IP_ADDR, vnpIpAddress);
        vnpParams.put(VNP_CREATE_DATE, DateFormatUtil.toString(now, appConf.getVnPay().getDatePattern()));
        vnpParams.put(VNP_EXPIRE_DATE, DateFormatUtil.toString(now.plus(appConf.getVnPay().getExpireTime(), ChronoUnit.SECONDS), appConf.getVnPay().getDatePattern()));
        vnpParams.put(VNP_ORDER_TYPE, appConf.getVnPay().getOrderType());
        log.info("create vnPay url success, amount: " + amount / appConf.getVnPay().getVnPayAmountRate());
        return vnpParams;
    }

    private String buildPaymentUrl(Map<String, Object> vnpParams, String secureHash) throws Exception {
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName).toString();
            if ((fieldValue != null) && (fieldValue.length() > 0)) {

                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayUtil.hmacSHA512(secureHash, hashData.toString());
        return String.format("%s?%s&%s=%s", appConf.getVnPay().getPayUrl(), queryUrl, VNP_SECURE_HASH, vnp_SecureHash);
    }

    public Object responseCustomerPayment(VnPayRequest request, String msgId) throws Exception {
        request.validate();
        Map<String, Object> vnpParams = new HashMap<>();
        vnpParams.put(VNP_TMN_CODE, request.getVnpTmnCode());
        vnpParams.put(VNP_TNX_REF, request.getVnpTxnRef());
        vnpParams.put(VNP_AMOUNT, request.getVnpAmount());
        vnpParams.put(VNP_ORDER_INFO, request.getVnpOrderInfo());
        vnpParams.put(VNP_RESPONSE_CODE, request.getVnpResponseCode());
        vnpParams.put(VNP_BANK_CODE, request.getVnpBankCode());
        vnpParams.put(VNP_BANK_TRAN_NO, request.getVnpBankTranNo());
        vnpParams.put(VNP_CARD_TYPE, request.getVnpCardType());
        vnpParams.put(VNP_PAY_DATE, request.getVnpPayDate());
        vnpParams.put(VNP_TRANSACTION_NO, request.getVnpTransactionNo());
        vnpParams.put(VNP_TRANSACTION_STATUS, request.getVnpTransactionStatus());
        if (!request.getVnpSecureHash().equals(VnPayUtil.hashAllFields(vnpParams, appConf.getVnPay().getPaymentInfo().getSecureHash()))) {
            this.saveVnPayTransactionPayment(request, null, "INVALID_CHECKSUM", null);
            throw new GeneralException("INVALID_CHECKSUM");
        }
        if (!VN_PAY_SUCCESS_CODE.equals(request.getVnpResponseCode())) {
            this.saveVnPayTransactionPayment(request, null, "PAYMENT_FAILED", null);
            throw new GeneralException("PAYMENT_FAILED");
        }
        Optional<Invoice> optionalInvoice = this.invoiceRepository.findById(request.getVnpTxnRef());
        if (!optionalInvoice.isPresent()) {
            this.saveVnPayTransactionPayment(request, null, "INVOICE_NOT_EXISTED", null);
            throw new GeneralException("INVOICE_NOT_EXISTED");
        }
        Double price = request.getVnpAmount() / appConf.getVnPay().getVnPayAmountRate();
        if (!optionalInvoice.get().getPrice().equals(price)) {
            log.info(msgId + "Actual proceed: " + optionalInvoice.get().getPrice() + ", price: " + price);
            this.saveVnPayTransactionPayment(request, null, "AMOUNT_DOES_NOT_MATCH_TO_INVOICE", optionalInvoice.get());
            throw new GeneralException("AMOUNT_DOES_NOT_MATCH_TO_INVOICE");
        }
        Optional<Transaction> optionalTransaction = this.transactionRepository.findByInvoiceIdAndStatusAndPartner(request.getVnpTxnRef(), TransactionStatus.PAYMENT_COMPLETE, TransactionPartner.VNPAY);
        if (optionalTransaction.isPresent()) {
            this.saveVnPayTransactionPayment(request, null, "VNP_TXN_REF_EXISTED_IN_DATABASE", optionalInvoice.get());
            throw new GeneralException("VNP_TXN_REF_EXISTED_IN_DATABASE");
        }
        Invoice invoice = optionalInvoice.get();
        invoice.setStatus(InvoiceStatus.PAYMENT_COMPLETE);
        this.invoiceRepository.save(invoice);
        Transaction transaction = this.saveVnPayTransactionPayment(request, optionalInvoice.get().getUserId(), null, optionalInvoice.get());
        this.repairer(transaction, invoice, request.getVnpOrderInfo(), optionalInvoice.get().getUserIdSideB(), msgId);
        Utils.sendNotification(transaction.getInvoice().getUserId(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<>();
    }

    public Object responseRepairerDeposit(VnPayRequest request, String msgId) throws IOException {
        request.validate();
        Map<String, Object> vnpParams = new HashMap<>();
        vnpParams.put(VNP_TMN_CODE, request.getVnpTmnCode());
        vnpParams.put(VNP_TNX_REF, request.getVnpTxnRef());
        vnpParams.put(VNP_AMOUNT, request.getVnpAmount());
        vnpParams.put(VNP_ORDER_INFO, request.getVnpOrderInfo());
        vnpParams.put(VNP_RESPONSE_CODE, request.getVnpResponseCode());
        vnpParams.put(VNP_BANK_CODE, request.getVnpBankCode());
        vnpParams.put(VNP_BANK_TRAN_NO, request.getVnpBankTranNo());
        vnpParams.put(VNP_CARD_TYPE, request.getVnpCardType());
        vnpParams.put(VNP_PAY_DATE, request.getVnpPayDate());
        vnpParams.put(VNP_TRANSACTION_NO, request.getVnpTransactionNo());
        vnpParams.put(VNP_TRANSACTION_STATUS, request.getVnpTransactionStatus());
        Transaction transaction = this.transactionRepository.findByInvoiceIdAndPartner(request.getVnpTxnRef(), TransactionPartner.VNPAY).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if (!request.getVnpSecureHash().equals(VnPayUtil.hashAllFields(vnpParams, appConf.getVnPay().getPaymentInfo().getSecureHash()))) {
            this.saveVnPayTransactionRepairer(request, transaction, transaction.getInvoice().getUserIdSideB(), "INVALID_CHECKSUM");
            throw new GeneralException("INVALID_CHECKSUM");
        }
        if (!VN_PAY_SUCCESS_CODE.equals(request.getVnpResponseCode())) {
            this.saveVnPayTransactionRepairer(request, transaction, transaction.getInvoice().getUserIdSideB(), "PAYMENT_FAILED");
            throw new GeneralException("PAYMENT_FAILED");
        }
        Optional<Invoice> optionalInvoice = this.invoiceRepository.findById(request.getVnpTxnRef());
        if (!optionalInvoice.isPresent()) {
            this.saveVnPayTransactionRepairer(request, transaction, transaction.getInvoice().getUserIdSideB(), "INVOICE_NOT_EXISTED");
            throw new GeneralException("INVOICE_NOT_EXISTED");
        }
        Double price = request.getVnpAmount() / appConf.getVnPay().getVnPayAmountRate();
        if (!optionalInvoice.get().getPrice().equals(price)) {
            log.info(msgId + "Actual proceed: " + optionalInvoice.get().getPrice() + ", price: " + price);
            this.saveVnPayTransactionRepairer(request, transaction, transaction.getInvoice().getUserIdSideB(), "AMOUNT_DOES_NOT_MATCH_TO_INVOICE");
            throw new GeneralException("AMOUNT_DOES_NOT_MATCH_TO_INVOICE");
        }
        Optional<Transaction> optionalTransaction = this.transactionRepository.findByInvoiceIdAndStatusAndPartner(request.getVnpTxnRef(), TransactionStatus.REPAIRER_COMPLETE, TransactionPartner.VNPAY);
        if (optionalTransaction.isPresent()) {
            this.saveVnPayTransactionRepairer(request, transaction, transaction.getInvoice().getUserIdSideB(), "VNP_TXN_REF_EXISTED_IN_DATABASE");
            throw new GeneralException("VNP_TXN_REF_EXISTED_IN_DATABASE");
        }
        Invoice invoice = optionalInvoice.get();
        invoice.setStatus(InvoiceStatus.DONE);
        this.invoiceRepository.save(invoice);
        this.saveVnPayTransactionRepairer(request, transaction, transaction.getInvoice().getUserIdSideB(), null);
        Utils.sendNotification(transaction.getInvoice().getUserIdSideB(), "", "", "", FirebaseType.TOKEN, null);
        return new HashMap<>();
    }

    private void saveVnPayTransactionRepairer(VnPayRequest request, Transaction transaction, String userId, String failReason) {
        transaction.setStatus(TransactionStatus.REPAIRER_COMPLETE);
        transaction = this.transactionRepository.save(transaction);
        this.saveTransactionHistory(request, transaction, userId, failReason, TransactionType.RECEIVE_INVOICE_MONEY);
    }

    private Transaction saveVnPayTransactionPayment(VnPayRequest request, String userId, String failReason, Invoice invoice) {
        Transaction transaction = this.saveTransaction(request, invoice);
        this.saveTransactionHistory(request, transaction, userId, failReason, TransactionType.CUSTOMER_PAYMENT);
        return transaction;
    }

    private Transaction saveTransaction(VnPayRequest request, Invoice invoice) {
        Transaction transaction = new Transaction();
        transaction.setPartner(TransactionPartner.VNPAY);
        transaction.setStatus(TransactionStatus.PAYMENT_COMPLETE);
        transaction.setAmount(request.getVnpAmount());
        transaction.setRequestCode(request.getVnpResponseCode());
        transaction.setRequestCode(request.getVnpTxnRef());
        transaction.setInvoice(invoice);
        return this.transactionRepository.save(transaction);
    }

    private void saveTransactionHistory(VnPayRequest request, Transaction transaction, String userId, String failReason, TransactionType type) {
        TransactionHistory transactionHistory = new TransactionHistory();
        transactionHistory.setUserId(userId);
        transactionHistory.setPartner(TransactionPartner.VNPAY);
        transactionHistory.setAmount(transactionHistory.getAmount());
        transactionHistory.setTransaction(transaction);
        transactionHistory.setRequestCode(transaction.getRequestCode());
        transactionHistory.setFailReason(failReason);
        transactionHistory.setType(type);
        transactionHistory.setStatus(StringUtils.isEmpty(failReason) ? TransactionHistoryStatus.SUCCESS : TransactionHistoryStatus.FAIL);
        transactionHistory.setTransactionNo(request.getVnpTransactionNo());
        transactionHistory.setTransactionStatus(request.getVnpTransactionStatus());
        transactionHistory.setBankCode(request.getVnpBankCode());
        transactionHistory.setBankTranNo(request.getVnpBankTranNo());
        transactionHistory.setCardType(request.getVnpCardType());
        transactionHistory.setResponseCode(request.getVnpResponseCode());
        this.transactionHistoryRepository.save(transactionHistory);
    }
}
