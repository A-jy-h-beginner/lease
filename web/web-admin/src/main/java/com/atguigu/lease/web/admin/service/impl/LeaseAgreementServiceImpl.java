package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.web.admin.mapper.LeaseAgreementMapper;
import com.atguigu.lease.web.admin.service.*;
import com.atguigu.lease.web.admin.vo.agreement.AgreementQueryVo;
import com.atguigu.lease.web.admin.vo.agreement.AgreementVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author liubo
 * @description 针对表【lease_agreement(租约信息表)】的数据库操作Service实现
 * @createDate 2023-07-24 15:48:00
 */
@Service
public class LeaseAgreementServiceImpl extends ServiceImpl<LeaseAgreementMapper, LeaseAgreement>
        implements LeaseAgreementService {

    @Autowired
    private LeaseAgreementMapper leaseAgreementMapper;
    @Autowired
    private ApartmentInfoService apartmentInfoService;
    @Autowired
    private RoomInfoService roomInfoService;
    @Autowired
    private PaymentTypeService paymentTypeService;
    @Autowired
    private LeaseTermService leaseTermService;
    @Override
    public IPage<AgreementVo> pageAgreement(Page<AgreementVo> page, AgreementQueryVo queryVo) {
       return leaseAgreementMapper.pageAgreement(page, queryVo);
    }

    @Override
    public AgreementVo getAgreementById(Long id) {
        // 获取签约信息
        LeaseAgreement leaseAgreement = lambdaQuery()
                .eq(LeaseAgreement::getId, id).one();
        // 获取签约公寓信息
        ApartmentInfo apartmentInfo = apartmentInfoService.lambdaQuery()
                .eq(ApartmentInfo::getId, leaseAgreement.getApartmentId())
                .one();
        // 获取签约房间信息
        RoomInfo roomInfo = roomInfoService.lambdaQuery()
                .eq(RoomInfo::getId, leaseAgreement.getRoomId())
                .one();
        // 获取支付方式
        PaymentType paymentType = paymentTypeService.lambdaQuery()
                .eq(PaymentType::getId, leaseAgreement.getPaymentTypeId())
                .one();
        // 获取租期
        LeaseTerm leaseTerm = leaseTermService.lambdaQuery()
                .eq(LeaseTerm::getId, leaseAgreement.getLeaseTermId())
                .one();
        // 组装
        AgreementVo agreementVo = new AgreementVo();
        BeanUtils.copyProperties(leaseAgreement, agreementVo);
        agreementVo.setApartmentInfo(apartmentInfo);
        agreementVo.setRoomInfo(roomInfo);
        agreementVo.setPaymentType(paymentType);
        agreementVo.setLeaseTerm(leaseTerm);
        return agreementVo;
    }
}




