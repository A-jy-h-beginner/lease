package com.atguigu.lease.web.admin.schedule;

import com.atguigu.lease.model.entity.LeaseAgreement;
import com.atguigu.lease.model.enums.LeaseStatus;
import com.atguigu.lease.web.admin.service.LeaseAgreementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ScheduleTask {
    @Autowired
    private LeaseAgreementService leaseAgreementService;
    @Scheduled(cron = "0 0 0 * * *")
    public void checkLeaseStatus(){  // 每天00：00定时更新在租，已过期
        leaseAgreementService.lambdaUpdate()
                .le(LeaseAgreement::getLeaseEndDate, new Date())
                .in(LeaseAgreement::getStatus, LeaseStatus.SIGNED, LeaseStatus.WITHDRAWING)
                .set(LeaseAgreement::getStatus, LeaseStatus.EXPIRED)
                .update();
    }
}
