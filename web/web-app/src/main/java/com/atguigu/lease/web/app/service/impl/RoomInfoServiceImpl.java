package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.common.constant.RedisConstant;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.login.LoginUserHolder;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.app.mapper.*;
import com.atguigu.lease.web.app.service.ApartmentInfoService;
import com.atguigu.lease.web.app.service.BrowsingHistoryService;
import com.atguigu.lease.web.app.service.PaymentTypeService;
import com.atguigu.lease.web.app.service.RoomInfoService;
import com.atguigu.lease.web.app.vo.apartment.ApartmentItemVo;
import com.atguigu.lease.web.app.vo.attr.AttrValueVo;
import com.atguigu.lease.web.app.vo.fee.FeeValueVo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import com.atguigu.lease.web.app.vo.room.RoomDetailVo;
import com.atguigu.lease.web.app.vo.room.RoomItemVo;
import com.atguigu.lease.web.app.vo.room.RoomQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liubo
 * @description 针对表【room_info(房间信息表)】的数据库操作Service实现
 * @createDate 2023-07-26 11:12:39
 */
@Service
@Slf4j
public class RoomInfoServiceImpl extends ServiceImpl<RoomInfoMapper, RoomInfo>
        implements RoomInfoService {
//    @Autowired 直接调用就好 循环依赖
//    private RoomInfoService roomInfoService;
    @Autowired
    private RoomInfoMapper roomInfoMapper;
    @Autowired
    private ApartmentInfoService apartmentInfoService;
    @Autowired
    private GraphInfoMapper graphInfoMapper;
    @Autowired
    private AttrValueMapper attrValueMapper;
    @Autowired
    private FacilityInfoMapper facilityInfoMapper;
    @Autowired
    private LabelInfoMapper labelInfoMapper;
    @Autowired
    private PaymentTypeMapper paymentTypeMapper;
    @Autowired
    private FeeValueMapper feeValueMapper;
    @Autowired
    private LeaseTermMapper leaseTermMapper;
    @Autowired
    private BrowsingHistoryService browsingHistoryService;
    @Autowired  // 导入自定义redisTemplate
    private RedisTemplate<String, Object> redisTemplate;
    @Override
    public IPage<RoomItemVo> pageItem(Page<RoomItemVo> page, RoomQueryVo queryVo) {
        return roomInfoMapper.pageItem(page, queryVo);
    }

    @Override
    public IPage<RoomItemVo> pageItemByApartmentId(Page<RoomItemVo> page, Long id) {
        return roomInfoMapper.pageItemByApartmentId(page, id);
    }

    @Override
    public RoomDetailVo getDetailById(Long id) {
        // 查询redis缓存
        String key = RedisConstant.APP_ROOM_PREFIX + id;
        RoomDetailVo roomDetailVo = (RoomDetailVo) redisTemplate.opsForValue().get(key);
        if(roomDetailVo == null){
            // 查询房间信息
            RoomInfo roomInfo = getById(id);
            if(roomInfo == null){
                throw new LeaseException(ResultCodeEnum.DATA_ERROR);
            }
            // 查询公寓信息
            ApartmentItemVo apartmentItemVo = apartmentInfoService.selectApartmentItemVoById(roomInfo.getApartmentId());
            // 查询图片
            List<GraphVo> graphVoList = graphInfoMapper.selectListByItemAndId(ItemType.ROOM, id);
            // 查询标签
            List<AttrValueVo> attrValueVoList = attrValueMapper.selectListByRoomId(id);
            // 查询配套
            List<FacilityInfo> facilityInfoList = facilityInfoMapper.selectListByRoomId(id);
            // 标签
            List<LabelInfo> labelInfoList = labelInfoMapper.selectByRoomId(id);
            // 支付方式
            List<PaymentType> paymentTypeList = paymentTypeMapper.selectListByRoomId(id);
            // 杂费 由所在公寓决定
            List<FeeValueVo> feeValueVoList = feeValueMapper.selectListByApartmentId(roomInfo.getApartmentId());
            // 租期
            List<LeaseTerm> leaseTermList = leaseTermMapper.selectListByRoomId(id);

            roomDetailVo = new RoomDetailVo();
            BeanUtils.copyProperties(roomInfo, roomDetailVo);
            roomDetailVo.setApartmentItemVo(apartmentItemVo);
            roomDetailVo.setGraphVoList(graphVoList);
            roomDetailVo.setAttrValueVoList(attrValueVoList);
            roomDetailVo.setFacilityInfoList(facilityInfoList);
            roomDetailVo.setLabelInfoList(labelInfoList);
            roomDetailVo.setPaymentTypeList(paymentTypeList);
            roomDetailVo.setFeeValueVoList(feeValueVoList);
            roomDetailVo.setLeaseTermList(leaseTermList);
            // 缓存到redis
            redisTemplate.opsForValue().set(key, roomDetailVo);
        }

        // 保存浏览历史
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        browsingHistoryService.History(userId, id);
        return roomDetailVo;
    }
}




