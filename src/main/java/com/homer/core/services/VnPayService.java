package com.homer.core.services;

import com.ea.async.Async;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.configurations.AppConf;
import com.homer.core.constants.Constants;
import com.homer.core.model.request.CustomerPaymentUrlRequest;
import com.homer.core.model.request.RepairerDepositUrlRequest;
import com.homer.core.model.response.UserInfo;
import com.homer.core.utils.DateFormatUtil;
import com.homer.core.utils.RandomUtil;
import com.homer.core.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.homer.core.constants.VnPayConstants.*;

@Service
@Slf4j
public class VnPayService {
    private final AppConf appConf;

    public VnPayService(
            AppConf appConf
    ) {
        this.appConf = appConf;
    }

    public Object createCustomerPaymentUrl(CustomerPaymentUrlRequest request, String msgId) {
        log.info("{} create customer payment url {}", msgId, request);
        request.validate();
        UserInfo userInfo = Async.await(Utils.getUserInfo(msgId, request.getHeaders().getToken().getUserData().getUserId()));
        if (!userInfo.getIsVerified()) {
            throw new GeneralException(Constants.USER_HADNT_BEEN_VERIFIED);
        }
//        Map<String, String> vnpParams = buildVnpParams(request.getOrderInfo(), request.getAmount(), request.getBankCode(), request.getSourceIp(), appConf.getVnPayInfo().getPaymentInfo().getTmnCode(), appConf.getVnPayInfo().getPaymentInfo().getReturnUrl(), request.getRequestCode());
        return new HashMap<>();
    }

    public Object createRepairerDepositUrl(RepairerDepositUrlRequest request, String msgId) {
        log.info("{} create repairer deposit url {}", msgId, request);
        request.validate();
        UserInfo userInfo = Async.await(Utils.getUserInfo(msgId, request.getHeaders().getToken().getUserData().getUserId()));
        if (!userInfo.getIsVerified()) {
            throw new GeneralException(Constants.USER_HADNT_BEEN_VERIFIED);
        }
        String txnRef = request.getHeaders().getToken().getUserData().getUserId() + RandomUtil.generateCode();
        Map<String, String> vnpParams = buildVnpParams(request.getOrderInfo(), request.getAmount(), request.getBankCode(), request.getSourceIp(), appConf.getVnPayInfo().getPaymentInfo().getTmnCode(), appConf.getVnPayInfo().getPaymentInfo().getReturnUrl(), txnRef);
        return new HashMap<>();
    }

    private Map<String, String> buildVnpParams(String vnpOrderInfo,
                                               Double amount,
                                               String bankCode,
                                               String vnpIpAddress,
                                               String vnpTmnCode,
                                               String returnUrl,
                                               String tnxRef) {
        LocalDateTime now = LocalDateTime.now();
        String vnpVersion = appConf.getVnPayInfo().getVersion();
        String vnpCommand = appConf.getVnPayInfo().getCommand();

        String locate = appConf.getVnPayInfo().getLocate();
        log.info("create vnPay url success, amount: " + amount / appConf.getVnPayInfo().getVnPayAmountRate());

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put(VNP_VERSION, vnpVersion);
        vnpParams.put(VNP_COMMAND, vnpCommand);
        vnpParams.put(VNP_TMN_CODE, vnpTmnCode);
        vnpParams.put(VNP_AMOUNT, String.valueOf(amount));
        vnpParams.put(VNP_CURR_CODE, appConf.getVnPayInfo().getCurrCode());
        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParams.put(VNP_BANK_CODE, bankCode);
        }
        vnpParams.put(VNP_TNX_REF, tnxRef);
        vnpParams.put(VNP_ORDER_INFO, vnpOrderInfo);
        vnpParams.put(VNP_LOCALE, locate);
        vnpParams.put(VNP_RETURN_URL, returnUrl);
        vnpParams.put(VNP_IP_ADDR, vnpIpAddress);
        vnpParams.put(VNP_CREATE_DATE, DateFormatUtil.toString(now, appConf.getVnPayInfo().getDatePattern()));

        return vnpParams;
    }
}
