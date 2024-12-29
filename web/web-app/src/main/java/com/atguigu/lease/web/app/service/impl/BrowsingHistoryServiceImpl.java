package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.common.login.LoginUserHolder;
import com.atguigu.lease.model.entity.BrowsingHistory;
import com.atguigu.lease.web.app.mapper.BrowsingHistoryMapper;
import com.atguigu.lease.web.app.service.BrowsingHistoryService;
import com.atguigu.lease.web.app.vo.history.HistoryItemVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author liubo
 * @description 针对表【browsing_history(浏览历史)】的数据库操作Service实现
 * @createDate 2023-07-26 11:12:39
 */
@Service
public class BrowsingHistoryServiceImpl extends ServiceImpl<BrowsingHistoryMapper, BrowsingHistory>
        implements BrowsingHistoryService {
    @Autowired
    private BrowsingHistoryMapper browsingHistoryMapper;
    @Override
    public IPage<HistoryItemVo> pageItem(Page<HistoryItemVo> page) {
        // 获取当前用户id
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        return  browsingHistoryMapper.pageItem(page, userId);
    }

    @Override
    @Async
    public void History(Long userId, Long roomId) {  // 保存历史
        // 同一用户浏览同一房间
        BrowsingHistory history = lambdaQuery()
                .eq(BrowsingHistory::getUserId, userId)
                .eq(BrowsingHistory::getRoomId, roomId)
                .one();
        if(history != null){ // 更新浏览时间
            lambdaUpdate()
                    .eq(BrowsingHistory::getId, history.getId())
                    .set(BrowsingHistory::getBrowseTime, new Date())
                    .update();
        }else {
            BrowsingHistory historyNew = new BrowsingHistory();
            historyNew.setUserId(userId);
            historyNew.setRoomId(roomId);
            historyNew.setBrowseTime(new Date());
            save(historyNew);
        }

    }
}