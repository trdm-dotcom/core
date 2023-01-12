package com.homer.core.services;

import com.ea.async.Async;
import com.homer.core.common.exceptions.GeneralException;
import com.homer.core.configurations.AppConf;
import com.homer.core.constants.Constants;
import com.homer.core.model.InvoiceStatus;
import com.homer.core.model.db.Invoice;
import com.homer.core.model.db.Post;
import com.homer.core.model.request.RequestingRepairRequest;
import com.homer.core.model.response.UserInfo;
import com.homer.core.repository.InvoiceRepository;
import com.homer.core.repository.PostRepository;
import com.homer.core.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
@Slf4j
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final PostRepository postRepository;
    private final AppConf appConf;

    @Autowired
    public InvoiceService(
            InvoiceRepository invoiceRepository,
            PostRepository postRepository,
            AppConf appConf
    ) {
        this.invoiceRepository = invoiceRepository;
        this.postRepository = postRepository;
        this.appConf = appConf;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Object createNewInvoice(RequestingRepairRequest request, String msgId){
        log.info("{} createInvoice {}", msgId, request);
        request.validate();
        UserInfo userInfo = Utils.getUserInfo(msgId, request.getHeaders().getToken().getUserData().getUserId());
        if(!userInfo.getIsVerified()){
            throw new GeneralException(Constants.CREATE_FAILED);
        }
        if (request.getDescription() != null && request.getDescription().length() > appConf.getDescriptionMaxLength()) {
            throw new GeneralException("EXCEEDED_DESCRIPTION_LENGTH_ALLOWED");
        }
        Post post = postRepository.findById(request.getPostId()).orElseThrow(() -> new GeneralException("OBJECT_NOT_FOUND"));
        if(!post.getIsPublic()){
            throw new GeneralException(Constants.CREATE_FAILED);
        }
        Invoice invoice = new Invoice();
        invoice.setUserId(request.getHeaders().getToken().getUserData().getUserId());
        invoice.setUserIdSideB(post.getUserId());
        invoice.setPost(post);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setPrice(post.getPrice() * post.getMinMonth());
        invoice.setDescription(request.getDescription());
        this.invoiceRepository.save(invoice);
        return new HashMap<>();
    }

}
